package fi.nls.oskari.map.layer.formatters;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.wms.WMSStyle;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.geometry.ProjectionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.wmts.domain.TileMatrixSet;
import fi.nls.oskari.wmts.domain.TileMatrixLink;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import static fi.nls.oskari.service.capabilities.CapabilitiesConstants.*;

public class LayerJSONFormatterWMTS extends LayerJSONFormatter {
    private static Logger log = LogFactory.getLogger(LayerJSONFormatterWMTS.class);

    public JSONObject getJSON(OskariLayer layer,
                              final String lang,
                              final boolean isSecure,
                              final String crs) {

        final JSONObject layerJson = getBaseJSON(layer, lang, isSecure, crs);

        String crsForTileMatrixSet = crs != null ? crs : layer.getSrs_name();
        String tileMatrixSetId = getTileMatrixSetId(layer.getCapabilities(), crsForTileMatrixSet);
        JSONHelper.putValue(layerJson, "tileMatrixSetId", tileMatrixSetId);

        // TODO: parse tileMatrixSetData for styles and set default style name from the one where isDefault = true
        String styleName = layer.getStyle();

        if(styleName == null || styleName.isEmpty()) {
            styleName = "default";
        }
        JSONHelper.putValue(layerJson, "style", styleName);
        try {
            JSONHelper.putValue(layerJson, KEY_STYLES, createStylesJSON(layer, isSecure));
        } catch (Exception e) {
            log.warn(e, "Populating layer styles failed for id: " + layer.getId());
        }

        // if options have urlTemplate -> use it (treat as a REST layer)
        final String urlTemplate = JSONHelper.getStringFromJSON(layer.getOptions(), "urlTemplate", null);
        final boolean needsProxy = useProxy(layer);
        if(urlTemplate != null) {
            if(needsProxy || isBeingProxiedViaOskariServer(layerJson.optString("url"))) {
                // remove requestEncoding so we always get KVP params when proxying
                JSONObject options = layerJson.optJSONObject("options");
                options.remove("requestEncoding");
            } else {
                // setup tileURL for REST layers
                final String originalUrl = layer.getUrl();
                layer.setUrl(urlTemplate);
                JSONHelper.putValue(layerJson, "tileUrl", layer.getUrl(isSecure));
                // switch back the original url in case it's used down the line
                layer.setUrl(originalUrl);
            }
        }

        Set<String> srs = getSRSs(layer.getAttributes(), layer.getCapabilities());
        if (srs != null) {
            JSONHelper.putValue(layerJson, KEY_SRS, new JSONArray(srs));
        }

        return layerJson;
    }

    /**
     * @deprecated use {@link #createCapabilitiesJSON(WMTSCapabilitiesLayer, Set)}
     */
    @Deprecated
    public static JSONObject createCapabilitiesJSON(final WMTSCapabilities wmts,final WMTSCapabilitiesLayer layer) {
        return createCapabilitiesJSON(layer, null);
    }

    /**
     * @deprecated use {@link #createCapabilitiesJSON(WMTSCapabilitiesLayer, Set)}
     */
    @Deprecated
    public static JSONObject createCapabilitiesJSON(final WMTSCapabilitiesLayer layer) {
        return createCapabilitiesJSON(layer, null);
    }

    public static JSONObject createCapabilitiesJSON(final WMTSCapabilitiesLayer layer, Set<String> systemCRSs) {
        JSONObject capabilities = new JSONObject();
        if (layer == null) {
            return capabilities;
        }

        List<JSONObject> tileMatrix = createTileMatrixArray(layer);
        JSONHelper.putValue(capabilities, KEY_TILEMATRIXIDS, new JSONArray(tileMatrix));

        final Set<String> capabilitiesCRSs = getCRSs(layer);
        final Set<String> crss = getCRSsToStore(systemCRSs, capabilitiesCRSs);
        JSONHelper.putValue(capabilities, KEY_SRS, new JSONArray(crss));

        JSONHelper.putValue(capabilities, KEY_FORMATS, getFormatsJSON(layer.getFormats()));
        JSONHelper.putValue(capabilities, KEY_ISQUERYABLE, layer.getInfoFormats().size() > 0);

        final JSONArray styles = new JSONArray();
        try {
            for(WMSStyle style : layer.getStyles()) {
                styles.put(style.toJSON());
            }
        } catch (Exception ignored) {}
        JSONHelper.put(capabilities, KEY_STYLES, styles);
        return capabilities;
    }

    /**
     * Return array of wmts tilematrixsets  (Crs code and Identifier)
     * @param layer wmts layer capabilities
     * @return
     */
    public static List<JSONObject> createTileMatrixArray(final WMTSCapabilitiesLayer layer) {
        final List<JSONObject> tileMatrix = new ArrayList<>();
        if (layer == null) {
            return tileMatrix;
        }

        for (TileMatrixLink link : layer.getLinks()) {
            TileMatrixSet tms = link.getTileMatrixSet();
            String identifier = tms.getId();
            String crs = tms.getCrs();
            String epsg = ProjectionHelper.shortSyntaxEpsg(crs);
            tileMatrix.add(JSONHelper.createJSONObject(epsg, identifier));
        }
        return tileMatrix;
    }

    /**
     * Constructs a Set containing the supported Coordinate Reference Systems of WMTS service
     */
    public static Set<String> getCRSs(final WMTSCapabilitiesLayer layer) {
        if (layer == null) {
            return null;
        }

        Set<String> crss = new HashSet<>();
        for (TileMatrixLink link : layer.getLinks()) {
            TileMatrixSet tms = link.getTileMatrixSet();
            String crs = tms.getCrs();
            String epsg = ProjectionHelper.shortSyntaxEpsg(crs);
            crss.add(epsg);
        }
        return crss;
    }

    /**
     * Get matrix id by current crs
     */
    public static String getTileMatrixSetId(final JSONObject capabilities, final String crs) {
        if (capabilities.has(KEY_TILEMATRIXIDS)) {
            JSONArray jsa = JSONHelper.getJSONArray(capabilities, KEY_TILEMATRIXIDS);

            for (int i = 0, size = jsa.length(); i < size; i++) {
                JSONObject js = JSONHelper.getJSONObject(jsa, i);
                if(js.has(crs)){
                    return JSONHelper.getStringFromJSON(js,crs,null);
                }
            }
        }
        return null;
    }

}
