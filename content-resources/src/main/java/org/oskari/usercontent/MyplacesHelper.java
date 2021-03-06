package org.oskari.usercontent;

import feign.FeignException;
import fi.nls.oskari.db.ConnectionInfo;
import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;


/**
 * Created by SMAKINEN on 4.9.2015.
 */
public class MyplacesHelper {

    public static final String MODULE_NAME = "myplaces";

    private static final Logger LOG = LogFactory.getLogger(MyplacesHelper.class);

    public static void setupMyplaces(final String srs) throws Exception {
        Geoserver geoserver = GeoserverPopulator.getGeoserver(MODULE_NAME);

        // Creating a namespace creates a workspace
        // (with ws you can only give a name, with ns you can also provide the uri)
        Namespace ns = new Namespace();
        try {

            ns.prefix = GeoserverPopulator.NAMESPACE;
            ns.uri = "http://www.oskari.org";
            geoserver.createNamespace(ns);
            LOG.info("Added namespace:", ns);
        } catch (FeignException ex) {
            LOG.error(ex, "Error adding namespace");
        }

        final String storeName = MODULE_NAME;
        try {
            DBDatastore ds = new DBDatastore();
            ds.name = storeName;


            DatasourceHelper helper = DatasourceHelper.getInstance();
            ConnectionInfo info = helper.getPropsForDS(MODULE_NAME);

            ds.connectionParameters.user = info.user;
            ds.connectionParameters.passwd = info.pass;
            ds.connectionParameters.host = info.getHost();
            ds.connectionParameters.port = info.getPort();
            ds.connectionParameters.database = info.getDBName();
            // in 2.5.2 namespace = NAMESPACE, in 2.7.1 it needs to be the uri?
            ds.connectionParameters.namespace = ns.uri;
            ds.addEntry("Loose bbox", "true");
            //System.out.println(mapper.writeValueAsString(ds));
            geoserver.createDBDatastore(ds, GeoserverPopulator.NAMESPACE);
            LOG.info("Added store:", ds);
        } catch (FeignException ex) {
            LOG.error(ex, "Error adding store");
        }

        // for data modification (WFS) - layers
        try {
            FeatureType featureCategories = new FeatureType();
            featureCategories.enabled = true;
            featureCategories.name = "categories";
            GeoserverPopulator.resolveCRS(featureCategories, srs);

            geoserver.createFeatureType(featureCategories, GeoserverPopulator.NAMESPACE, storeName);
            LOG.info("Added featuretype:", featureCategories);
        } catch (FeignException ex) {
            LOG.error(ex, "Error adding featuretype categories");
        }

        // for data modification (WFS) - places
        try {
            FeatureType featurePlaces = new FeatureType();
            featurePlaces.enabled = true;
            featurePlaces.name = "my_places";
            GeoserverPopulator.resolveCRS(featurePlaces, srs);

            geoserver.createFeatureType(featurePlaces, GeoserverPopulator.NAMESPACE, storeName);
            LOG.info("Added featuretype:", featurePlaces);
        } catch (FeignException ex) {
            LOG.error(ex, "Error adding featuretype my_places");
        }
    }
}
