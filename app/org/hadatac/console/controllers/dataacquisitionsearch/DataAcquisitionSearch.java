package org.hadatac.console.controllers.dataacquisitionsearch;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;
import module.DatabaseExecutionContext;
import org.apache.commons.lang.StringUtils;
import org.hadatac.Constants;
import org.hadatac.annotations.SearchActivityAnnotation;
import org.hadatac.console.controllers.Application;
import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.controllers.triplestore.UserManagement;
import org.hadatac.console.models.*;
import org.hadatac.console.views.html.dataacquisitionsearch.dataacquisition_browser;
import org.hadatac.console.views.html.dataacquisitionsearch.facetOnlyBrowser;
import org.hadatac.data.model.AcquisitionQueryResult;
import org.hadatac.entity.pojo.Measurement;
import org.hadatac.entity.pojo.ObjectCollection;
import org.hadatac.entity.pojo.SPARQLUtilsFacetSearch;
import org.hadatac.entity.pojo.User;
import org.hadatac.utils.ConfigProp;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@SearchActivityAnnotation()
public class DataAcquisitionSearch extends Controller {

    private static final Logger log = LoggerFactory.getLogger(DataAcquisitionSearch.class);

    @Inject
    HttpExecutionContext ec;
    @Inject
    DatabaseExecutionContext databaseExecutionContext;
    @Inject
    Application application;

    public static FacetFormData facet_form = new FacetFormData();
    public static FacetsWithCategories field_facets = new FacetsWithCategories();
    public static FacetsWithCategories query_facets = new FacetsWithCategories();
    public static FacetsWithCategories pivot_facets = new FacetsWithCategories();
    public static FacetsWithCategories range_facets = new FacetsWithCategories();
    public static FacetsWithCategories cluster_facets = new FacetsWithCategories();
    public static SpatialQueryResults query_results = new SpatialQueryResults();

    Map<String, String> studyFacetQueries;

    // looks like no one is calling this
    public static List<String> getPermissions(String permissions) {
        List<String> result = new ArrayList<String>();

        if (permissions != null) {
            StringTokenizer tokens = new StringTokenizer(permissions, ",");
            while (tokens.hasMoreTokens()) {
                result.add(tokens.nextToken());
            }
        }

        return result;
    }

    private static ObjectDetails getObjectDetails(AcquisitionQueryResult results) {
        Set<String> setObj = new HashSet<String>();
        ObjectDetails objDetails = new ObjectDetails();
        if (results != null) {
            for (Measurement m : results.getDocuments()) {
                setObj.add(m.getObjectUri());
            }
            for (String uri : setObj) {
                if (uri != null) {
                    // NEEDS TO REPLACE WITH VIEWSTUDYOBJECT
                    //String html = ViewSubject.findBasicHTML(uri);
                    //if (html != null) {
                    //    objDetails.putObject(uri, html);
                    //}
                }
            }
        }

        return objDetails;
    }

    // @Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result index(int page, int rows, Http.Request request) {

        //printMemoryStats();
        /*long startTime = System.currentTimeMillis();
        Model model = SPARQLUtilsFacetSearch.createInMemoryModel();
        System.out.println("in-memory model created, taking " + (System.currentTimeMillis()-startTime) + "ms, with # of triples = " + model.size());
        */
        //printMemoryStats();

        SPARQLUtilsFacetSearch.clearCache();
        // SolrUtilsFacetSearch.clearCache();

        return indexInternalAsync(0, page, rows, request);

        /*
        if ( "ON".equalsIgnoreCase(ConfigFactory.load().getString("hadatac.facet_search.concurrency")) ) {
            log.debug("using async calls for facet search....");
            return indexInternalAsync(0, page, rows,request);
        } else {
            return indexInternal(0, page, rows,request);
        }
         */
    }

    // @Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result postIndex(int page, int rows, Http.Request request) {
        return index(page, rows, request);
    }

    /*
    // @Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result indexData(int page, int rows, Http.Request request) {
        return indexInternal(1, page, rows, request);
    }
     */

    /*
    // @Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result postIndexData(int page, int rows, Http.Request request) {
        return indexData(page, rows, request);
    }
     */

    /*
    private Result indexInternal(int mode, int page, int rows, Http.Request request) {
        String facets = "";
        if (request.body().asFormUrlEncoded() != null) {
            facets = request.body().asFormUrlEncoded().get("facets")[0];
        }

        //System.out.println("\n\n\n\n\nfacets: " + facets);

        FacetHandler facetHandler = new FacetHandler();
        facetHandler.loadFacetsFromString(facets);

        AcquisitionQueryResult results = null;
        String ownerUri;
        final SysUser user = AuthApplication.getLocalUser(application.getUserEmail(request));
        if (null == user) {
            ownerUri = "Public";
        }
        else {
            ownerUri = UserManagement.getUriByEmail(user.getEmail());
            if (null == ownerUri){
                ownerUri = "Public";
            }
        }
        //System.out.println("OwnerURI: " + ownerUri);

        results = Measurement.findAsync(ownerUri, page, rows, facets);

        ObjectDetails objDetails = getObjectDetails(results);

        //System.out.println("\n\n\n\nresults to JSON: " + results.toJSON());
        List<ObjectCollection> objectCollections = ObjectCollection.findAllFacetSearch();

        SPARQLUtilsFacetSearch.reportStats();
        // SolrUtilsFacetSearch.reportStats();

        if (mode == 0) {
            return ok(facetOnlyBrowser.render(page, rows, ownerUri, facets, results.getDocumentSize(),
                    results, results.toJSON(), facetHandler, objDetails.toJSON(),
                    Measurement.getFieldNames(), objectCollections,application.getUserEmail(request)));
        } else {
            return ok(dataacquisition_browser.render(page, rows, ownerUri, facets, results.getDocumentSize(),
                    results, results.toJSON(), facetHandler, objDetails.toJSON(),
                    Measurement.getFieldNames(), objectCollections,application.getUserEmail(request)));
        }
    }
     */

    private Result indexInternalAsync(int mode, int page, int rows, Http.Request request) {

        Boolean initialQuery = false;

        try {
            String facets = "";
            if (request.body().asFormUrlEncoded() != null) {
                facets = request.body().asFormUrlEncoded().get("facets")[0];
            }

            System.out.println("indexInternalAsync: facets=[" + facets + "]");

            if (facets == null || facets.isEmpty() || facets.equals(FacetHandler.DEFAULT_FACET)) {
                initialQuery = true;
            }

            // log.debug("facets: " + facets);

            FacetHandler facetHandler = new FacetHandler();
            facetHandler.loadFacetsFromString(facets);

            AcquisitionQueryResult results = null;
            String ownerUri;

            long startTime = System.currentTimeMillis();
            final SysUser user = AuthApplication.getLocalUser(application.getUserEmail(request));
            Measurement.setCurrentUser(user);
            log.debug("---> AuthApplication.getLocalUser() takes " + (System.currentTimeMillis() - startTime) + "sms to finish");

            if (null == user) {
                ownerUri = "Public";
            } else {
                ownerUri = UserManagement.getUriByEmail(user.getEmail());
                if (null == ownerUri) {
                    ownerUri = "Public";
                }
            }

            // do the following two concurrently

            startTime = System.currentTimeMillis();

            CompletableFuture<List<ObjectCollection>> promiseOfObjs = CompletableFuture.supplyAsync((
                    () -> {
                        return ObjectCollection.findAllFacetSearch();
                    }
            ), databaseExecutionContext);

            String finalFacets = facets;
            String finalOwnerUri = ownerUri;
            Boolean finalInitialQuery = initialQuery;
            CompletableFuture<AcquisitionQueryResult> promiseOfFacetStats = CompletableFuture.supplyAsync((
                    () -> {
                        return Measurement.findAsync(finalInitialQuery, finalOwnerUri, page, rows, finalFacets, databaseExecutionContext);
                    }
            ), databaseExecutionContext);


            List<String> fileNames = Measurement.getFieldNames();
            log.debug("---> Measurement.getFieldNames() takes " + (System.currentTimeMillis() - startTime) + "sms to finish");

            List<ObjectCollection> objs = null;

            objs = promiseOfObjs.get();

            results = promiseOfFacetStats.get();

            log.debug("---> ObjectCollection.findAllFacetSearch() + Measurement.findAsync() takes " + (System.currentTimeMillis() - startTime) + "sms to finish");

            startTime = System.currentTimeMillis();

            // System.out.println("\n\n\n\nresults to JSON: " + results.toJSON());
            ObjectDetails objDetails = getObjectDetails(results);
            log.debug("---> getObjectDetails() takes " + (System.currentTimeMillis() - startTime) + "sms to finish");

            // System.out.println("\n\n\n\nresults to JSON 1: " + results.toJSON());

            SPARQLUtilsFacetSearch.reportStats();
            // SolrUtilsFacetSearch.reportStats();

            if (mode == 0) {
                return ok(facetOnlyBrowser.render(page, rows, ownerUri, facets, results.getDocumentSize(),
                        results, results.toJSON(), facetHandler, objDetails.toJSON(),
                        fileNames, objs, application.getUserEmail(request)));
            } else {
                return ok(dataacquisition_browser.render(page, rows, ownerUri, facets, results.getDocumentSize(),
                        results, results.toJSON(), facetHandler, objDetails.toJSON(),
                        fileNames, objs, application.getUserEmail(request)));
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ok();

    }

    /*
    //@Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result download(Http.Request request) {
        System.out.println("inside download");
        String ownerUri = getOwnerUri(request);
        String email = getUserEmail(request);

        String facets = "";
        List<String> selectedFields = new LinkedList<String>();
        Map<String, String[]> name_map = request.body().asFormUrlEncoded();
        if (name_map != null) {
            facets = name_map.get("facets")[0];

            List<String> keys = new ArrayList<String>(name_map.keySet());
            keys.remove("facets");

            selectedFields.addAll(keys);
        }
        //System.out.println("selectedFields: " + selectedFields);

        AcquisitionQueryResult results = Measurement.findAsync(ownerUri, -1, -1, facets);

        final String finalFacets = facets;
        CompletableFuture.supplyAsync(() -> Downloader.generateCSVFile(
                results.getDocuments(), finalFacets, selectedFields, email),
                ec.current());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return redirect(routes.Downloader.index());
    }
     */

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result downloadAlignment(Http.Request request) {
        System.out.println("DataAcquisitionSearch.downloadAlignment : request=[" + request + "]");
        String ownerUri = getOwnerUri(request);
        String email = getUserEmail(request);

        String facets = "";
        String objectType = "";
        String categoricalValues = "";
        String timeResolution = "";
        String sameValueSelection = "";
        boolean renameFiles = false;

        List<String> selectedFields = new LinkedList<String>();
        Map<String, String[]> name_map = request.body().asFormUrlEncoded();
        System.out.println("DataAcquisitionSearch.downloadAlignment : name_map=[" + name_map + "]");

        name_map.forEach((key, value) -> {
            System.out.println("DataAcquisitionSearch.downloadAlignment Key : " + key + " Value : " + value);
        });

        if (name_map != null) {
            if (name_map.get("facets") != null) {
                facets = name_map.get("facets")[0];
            }

            //System.out.println("DataAcquisitionSearch.downloadAlignment : name_map=[" + name_map + "]");
            if (name_map.get("downloadSrc") != null) {
                String downloadSrc = name_map.get("downloadSrc")[0].toString();
                if (downloadSrc.equals("studypage")) {
                    if (name_map.get("studyIds") != null) {
                        //System.out.println("DataAcquisitionSearch.downloadAlignment : studyIds=[" + name_map.get("studyIds")[0] + "]");
                        String studyIds = name_map.get("studyIds")[0].toString();
                        System.out.println("DataAcquisitionSearch.downloadAlignment : studyIds=[" + studyIds + "]");

                        if (studyIds.contains("!")) {
                            String[] studyIdsArr = studyIds.split("!");
                            System.out.println("DataAcquisitionSearch.downloadAlignment : studyIdsArr=[" + studyIdsArr + "]");
                            //System.out.println("DataAcquisitionSearch.downloadAlignment : studyIdsArr length =[" + studyIdsArr.length + "]");
                            facets = getStudyFacetQuery(studyIdsArr);
                        } else {
                            //facets = ConfigProp.getFacetedSearhQuery("STD-"+studyIds);
                            System.out.println("DataAcquisitionSearch.downloadAlignment : studyId=[" + studyIds + "]");
                            facets = getStudyFacetQuery("STD-" + studyIds);
                        }
                    }
                }
            }

            if (StringUtils.isBlank(facets)) {
                System.out.println("DataAcquisitionSearch.downloadAlignment - Warning: missing facets information in form.");
            }

            if (name_map.get("selObjectType") != null) {
                objectType = name_map.get("selObjectType")[0].toString();
            }
            if (name_map.get("selCatValue") != null) {
                categoricalValues = name_map.get("selCatValue")[0].toString();
            }
            if (name_map.get("selTimeRes") != null) {
                timeResolution = name_map.get("selTimeRes")[0].toString();
            }
            if (name_map.get("selDupOpt") != null) {
                sameValueSelection = name_map.get("selDupOpt")[0].toString();
            }
            if (name_map.get("renameFiles") != null && name_map.get("renameFiles")[0].toString().equals("true")) {
                renameFiles = true;
            }
        }

        System.out.println("DataAcquisitionSearch.downloadAlignment : facets=[" + facets + "]");

        long startTime = System.currentTimeMillis();
        // AcquisitionQueryResult results = Measurement.findAsync(ownerUri, -1, -1, facets,databaseExecutionContext);
        AcquisitionQueryResult results = null;
        log.debug("DOWNLOAD: Measurement find takes " + (System.currentTimeMillis() - startTime) + "ms to finish");

        final String finalFacets = facets;
        final String categoricalOption = categoricalValues;
        final String timeOption = timeResolution;
        final boolean finalRenameFiles = renameFiles;
        final boolean keepSameValue = "eliminateDuplication".equalsIgnoreCase(sameValueSelection) ? false : true;
        //System.out.println("Object type inside alignment: " + objectType);

        CompletionStage<Integer> promiseOfResult = null;
        long currentTime = System.currentTimeMillis();
        if (objectType.equals(Downloader.ALIGNMENT_SUBJECT)) {
            //System.out.println("Selected subject alignment");
            promiseOfResult = CompletableFuture.supplyAsync(() -> Downloader.generateCSVFileBySubjectAlignment(
                    ownerUri, finalFacets, email, Measurement.SUMMARY_TYPE_NONE, categoricalOption, finalRenameFiles, keepSameValue, null),
                    databaseExecutionContext);
        } else if (objectType.equals(Downloader.ALIGNMENT_TIME)) {
            //System.out.println("Selected time alignment");
            promiseOfResult = CompletableFuture.supplyAsync(() -> Downloader.generateCSVFileByTimeAlignment(
                    results.getDocuments(), finalFacets, email, categoricalOption, timeOption),
                    databaseExecutionContext);
        }

        promiseOfResult.whenComplete(
                (result, exception) -> {
                    log.debug("DOWNLOAD: downloading DA files is done, taking " + (System.currentTimeMillis() - currentTime) + "ms to finish");
                });

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return redirect(org.hadatac.console.controllers.workingfiles.routes.WorkingFiles.index_datasetGeneration("/", "/", false));
    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result downloadSummarization(Http.Request request) {
        String ownerUri = getOwnerUri(request);
        String email = getUserEmail(request);

        String facets = "";
        String selSummaryType = "";
        String nonCategoricalVariables = "";
        boolean renameFiles = false;

        List<String> selectedFields = new LinkedList<String>();
        Map<String, String[]> name_map = request.body().asFormUrlEncoded();
        if (name_map != null) {
            if (name_map.get("facets") != null) {
                facets = name_map.get("facets")[0];
            }
            if (name_map.get("selSummaryType") != null) {
                selSummaryType = name_map.get("selSummaryType")[0].toString();
            }
            if (name_map.get("selNonCatVariable") != null) {
                nonCategoricalVariables = name_map.get("selNonCatVariable")[0].toString();
            }
            if (name_map.get("renameFiles") != null && name_map.get("renameFiles")[0].toString().equals("true")) {
                renameFiles = true;
            }
        }

        //System.out.println("DataAcquisitionSearch.downloadSummarization : name_map=[" + name_map.get("facets")[0] + "]");
        System.out.println("DataAcquisitionSearch.downloadSummarization : facets=[" + facets + "]");

        long startTime = System.currentTimeMillis();
        // AcquisitionQueryResult results = Measurement.findAsync(ownerUri, -1, -1, facets,databaseExecutionContext);
        AcquisitionQueryResult results = null;
        log.debug("DOWNLOAD: Measurement find takes " + (System.currentTimeMillis() - startTime) + "ms to finish");

        final String finalFacets = facets;
        final String summaryType = selSummaryType;
        final String categoricalOption = nonCategoricalVariables;
        final boolean finalRenameFiles = renameFiles;

        CompletionStage<Integer> promiseOfResult = null;
        long currentTime = System.currentTimeMillis();

        if (selSummaryType.equals(Measurement.SUMMARY_TYPE_SUBGROUP)) {
            // for TYPE_SUBGROUP, keepSameValue is set to 'false'
            promiseOfResult = CompletableFuture.supplyAsync(() -> Downloader.generateCSVFileBySubjectAlignment(
                    ownerUri, finalFacets, email, summaryType, categoricalOption, finalRenameFiles, false, null),
                    databaseExecutionContext);

            promiseOfResult.whenComplete(
                    (result, exception) -> {
                        log.debug("DOWNLOAD: downloading DA files is done, taking " + (System.currentTimeMillis() - currentTime) + "ms to finish");
                    });

        } else {

            promiseOfResult = CompletableFuture.supplyAsync(() -> Downloader.generateCSVFileBySummarization(
                    ownerUri, finalFacets, email, summaryType, categoricalOption, null),
                    databaseExecutionContext);

            promiseOfResult.whenComplete(
                    (result, exception) -> {
                        log.debug("DOWNLOAD: downloading DA files is done, taking " + (System.currentTimeMillis() - currentTime) + "ms to finish");
                    });
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return redirect(org.hadatac.console.controllers.workingfiles.routes.WorkingFiles.index_datasetGeneration("/", "/", false));
    }

    private String getUserEmail(Http.Request request) {
        final SysUser user = AuthApplication.getLocalUser(application.getUserEmail(request));
        if (null != user) {
            return user.getEmail();
        }

        return "";
    }

    private String getOwnerUri(Http.Request request) {
        String ownerUri = "";
        final SysUser user = AuthApplication.getLocalUser(application.getUserEmail(request));
        if (null == user) {
            ownerUri = "Public";
        } else {
            ownerUri = UserManagement.getUriByEmail(user.getEmail());
            if (null == ownerUri) {
                ownerUri = "Public";
            }
        }

        return ownerUri;
    }

    /*
    @Secure (authorizers = Constants.DATA_OWNER_ROLE)
    public Result postDownload(Http.Request request) {
        return download(request);
    }
     */

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result postDownloadAlignment(Http.Request request) {
        return downloadAlignment(request);
    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result postDownloadSummarization(Http.Request request) {
        return downloadSummarization(request);
    }

    private static void printMemoryStats() {
        /* Total number of processors or cores available to the JVM */
        System.out.println("Available processors (cores): " +
                Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        System.out.println("Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently in use by the JVM */
        System.out.println("Total memory (bytes): " +
                Runtime.getRuntime().totalMemory());

    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result preferences(Boolean fs, Boolean fo, Boolean fec, Boolean fu, Boolean ft, Boolean fsp, Boolean fp, Http.Request request) {
        String userEmail = application.getUserEmail(request);
        User user = User.findByEmail(userEmail);
        if (user != null) {
            if (fs == true) {
                user.setFacetedDataStudy("on");
            } else {
                user.setFacetedDataStudy("off");
            }
            if (fo == true) {
                user.setFacetedDataObject("on");
            } else {
                user.setFacetedDataObject("off");
            }
            if (fec == true) {
                user.setFacetedDataEntityCharacteristic("on");
            } else {
                user.setFacetedDataEntityCharacteristic("off");
            }
            if (fu == true) {
                user.setFacetedDataUnit("on");
            } else {
                user.setFacetedDataUnit("off");
            }
            if (ft == true) {
                user.setFacetedDataTime("on");
            } else {
                user.setFacetedDataTime("off");
            }
            if (fsp == true) {
                user.setFacetedDataSpace("on");
            } else {
                user.setFacetedDataSpace("off");
            }
            if (fp == true) {
                user.setFacetedDataPlatform("on");
            } else {
                user.setFacetedDataPlatform("off");
            }
            user.updateFacetPreferences();
        }
        return redirect(org.hadatac.console.controllers.dataacquisitionsearch.routes.DataAcquisitionSearch.index(1, 15));
    }

    @Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result postPreferences(Boolean fs, Boolean fo, Boolean fec, Boolean fu, Boolean ft, Boolean fsp, Boolean fp, Http.Request request) {
        return preferences(fs, fo, fec, fu, ft, fsp, fp, request);
    }

    private String getStudyFacetQuery(String studyId) {
        if (studyFacetQueries == null) {
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, List<String>> map = createStudyFacetDAFiles();
            studyFacetQueries = new HashMap<String, String>();

            map.forEach((study, das) -> {
                FacetS facetS = new FacetS();
                facetS.setId("http://hadatac.org/kb/hhear#" + study);
                facetS.setStudy_uri_str("http://hadatac.org/kb/hhear#" + study);

                List<DAChild> lst = new ArrayList<>();
                das.forEach((da) -> {
                    DAChild c = new DAChild();
                    c.setId(da);
                    c.setAcquisition_uri_str(da);
                    lst.add(c);
                });
                facetS.setChildren(lst);

                try {
                    String facetSJson = objectMapper.writeValueAsString(facetS);
                    //System.out.println("---------------\nfacetSJson:" +study + "\n" + facetSJson.toString());

                    StringBuilder facets = new StringBuilder();
                    facets.append("{");
                    facets.append("\"facetsS\":[");
                    facets.append(facetSJson);
                    facets.append("],");
                    facets.append("\"facetsEC\":[],");
                    facets.append("\"facetsOC\":[],");
                    facets.append("\"facetsU\":[],");
                    facets.append("\"facetsT\":[],");
                    facets.append("\"facetsPI\":[]");
                    facets.append("}");

                    //System.out.println("\n-----Build FACET----------\nStudy:" +study + "\n" + facets.toString());

                    studyFacetQueries.put(study, facets.toString());
                } catch (Exception e) {
                }
            });
        }

        String newFacetQuery = studyFacetQueries.get(studyId);
        System.out.println("\n-----NEW VERSION----------\nStudy:" + studyId + "\n" + newFacetQuery);

        String oldFacetQuery = getStudyFacetQuery2(studyId);
        System.out.println("\n-----OLD VERSION----------\nStudy:" + studyId + "\n" + oldFacetQuery);
        System.out.println();

        return studyFacetQueries.get(studyId);
        //return oldFacetQuery;
    }

    private String getStudyFacetQuery2(String studyId) {
        //if(studyFacetQueries==null)
        //{
        HashMap<String, String> studyFacetQueries = new HashMap<String, String>();

        studyFacetQueries.put("STD-2016-34", "{\"facetsEC\":[],\"facetsS\":[{\"id\":\"http://hadatac.org/kb/hhear#STD-2016-34\",\"study_uri_str\":\"http://hadatac.org/kb/hhear#STD-2016-34\",\"children\":[{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-34-Lab-Creatinine\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-34-Lab-Creatinine\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-34-Lab-Metals\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-34-Lab-Metals\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-34-PD-DemoHealth\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-34-PD-DemoHealth\"}]}],\"facetsOC\":[],\"facetsU\":[],\"facetsT\":[],\"facetsPI\":[]}");
        studyFacetQueries.put("STD-2016-1431", "{\"facetsEC\":[],\"facetsS\":[{\"id\":\"http://hadatac.org/kb/hhear#STD-2016-1431\",\"study_uri_str\":\"http://hadatac.org/kb/hhear#STD-2016-1431\",\"children\":[{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-PSAMPLES\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-PSAMPLES\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-SSAMPLES\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-SSAMPLES\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P01\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P01\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P20\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P20\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Demo\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Demo\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Exposures-P20\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Exposures-P20\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P01\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P01\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P20\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P20\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-SpecGrav-P01\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2016-1431-PD-SpecGrav-P01\"}]}],\"facetsOC\":[],\"facetsU\":[],\"facetsT\":[],\"facetsPI\":[]}");
        studyFacetQueries.put("STD-2017-1762", "{\"facetsEC\":[],\"facetsS\":[{\"id\":\"http://hadatac.org/kb/hhear#STD-2017-1762\",\"study_uri_str\":\"http://hadatac.org/kb/hhear#STD-2017-1762\",\"children\":[{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotA\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotA\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotB\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotB\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-Inflam\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-1762-Lab-Inflam\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-1762-PD-DemoHealth\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-1762-PD-DemoHealth\"}]}],\"facetsOC\":[],\"facetsU\":[],\"facetsT\":[],\"facetsPI\":[]}");
        studyFacetQueries.put("STD-2017-2121", "{\"facetsEC\":[],\"facetsS\":[{\"id\":\"http://hadatac.org/kb/hhear#STD-2017-2121\",\"study_uri_str\":\"http://hadatac.org/kb/hhear#STD-2017-2121\",\"children\":[{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Creatinine\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Creatinine\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Inflam\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Inflam\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Oxid\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Oxid\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Phthalates\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Phthalates\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPAH\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPAH\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_DAPs\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_DAPs\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_UPMs\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_UPMs\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPHENOL_UPB\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPHENOL_UPB\"},{\"id\":\"http://hadatac.org/kb/hhear#DA-2017-2121-PD-DemoHealth\",\"acquisition_uri_str\":\"http://hadatac.org/kb/hhear#DA-2017-2121-PD-DemoHealth\"}]}],\"facetsOC\":[],\"facetsU\":[],\"facetsT\":[],\"facetsPI\":[]}");
        //}

        return studyFacetQueries.get(studyId);
    }

    private String getStudyFacetQuery(String[] studyIds) {
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, List<String>> map = createStudyFacetDAFiles();
        String studyFacetQuery = "";

        List<FacetS> facetSet = new ArrayList<>();

        List<String> studIdList = Arrays.asList(studyIds);
        //System.out.println("DataAcquisitionSearch.downloadAlignment : studIdList=[" + studIdList + "]");
        //System.out.println("DataAcquisitionSearch.downloadAlignment : studIdList size =[" + studIdList.size() + "]");

        //studIdList.forEach((id) -> System.out.print(id + ",  "));
        //System.out.println();

        map.forEach((study, das) -> {
            //System.out.println("DataAcquisitionSearch.downloadAlignment : study=[" + study + "]");
            //System.out.println("DataAcquisitionSearch.downloadAlignment : das=[" + das + "]");
            String studyNumber = study.replace("STD-", "");
            //System.out.println("DataAcquisitionSearch.downloadAlignment : studyNumber=[" + studyNumber + "]");
            if (studIdList.contains(studyNumber)) {
                FacetS facetS = new FacetS();
                facetS.setId("http://hadatac.org/kb/hhear#" + study);
                facetS.setStudy_uri_str("http://hadatac.org/kb/hhear#" + study);

                List<DAChild> lst = new ArrayList<>();
                das.forEach((da) -> {
                    DAChild c = new DAChild();
                    c.setId(da);
                    c.setAcquisition_uri_str(da);
                    lst.add(c);
                });
                facetS.setChildren(lst);
                facetSet.add(facetS);
            }
        });

        try {
            String facetSJson = objectMapper.writeValueAsString(facetSet);

            StringBuilder facets = new StringBuilder();
            facets.append("{");
            facets.append("\"facetsS\":");
            facets.append(facetSJson);
            facets.append(",");
            facets.append("\"facetsEC\":[],");
            facets.append("\"facetsOC\":[],");
            facets.append("\"facetsU\":[],");
            facets.append("\"facetsT\":[],");
            facets.append("\"facetsPI\":[]");
            facets.append("}");

            //System.out.println("\n-----Build FACET----------\n" + facets.toString());

            studyFacetQuery = facets.toString();
        } catch (Exception e) {
        }

        return studyFacetQuery;
    }

    private Map<String, List<String>> createStudyFacetDAFiles() {

        List<String> csv = getDAFilesFromTripleStore();

        Map<String, List<String>> map = new HashMap<String, List<String>>();


        csv.forEach(das -> {
            String key = das.split(",")[0];
            String da = das.split(",")[1];
            if (map.containsKey(key)) {
                List list = map.get(key);
                list.add(da);
            } else {
                List list = new ArrayList<>();
                list.add(da);
                map.put(key, list);
            }
        });
        return map;
    }

    private List<String> getDAFilesFromTripleStore() {
        List<String> csv = new ArrayList<>();

        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-NNAL");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-Cot");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-UEP");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-Creatinine");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-Phthalates");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-PAH");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-PD-DemoHealth");
        csv.add("STD-2016-1407,http://hadatac.org/kb/hhear#DA-2016-1407-Lab-SpecGrav");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P01");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P20");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Anthropom-P20");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-SSAMPLES");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-Lab-PAH-PSAMPLES");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Demo");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-SpecGrav-P01");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Metabolic-P01");
        csv.add("STD-2016-1431,http://hadatac.org/kb/hhear#DA-2016-1431-PD-Exposures-P20");
        csv.add("STD-2016-1432,http://hadatac.org/kb/hhear#DA-2016-1432-Lab-Inflam");
        csv.add("STD-2016-1432,http://hadatac.org/kb/hhear#DA-2016-1432-Lab-Metals");
        csv.add("STD-2016-1432,http://hadatac.org/kb/hhear#DA-2016-1432-PD-DemoHealth");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-ADOS");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-Mullen");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-Demo");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-Outcome");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-Covars-3");
        csv.add("STD-2016-1438,http://hadatac.org/kb/hhear#DA-2016-1438-PD-Covars-2");
        csv.add("STD-2016-1448,http://hadatac.org/kb/hhear#DA-2016-1448-PD-DemoHealth");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-Lab-Pesticides-T1");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-Lab-UEP-T2T3");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-Mullen");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-Lab-UEP-T1");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-Outcome");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-ADOS");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-ExposureT2T3");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-Demo");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-Lab-Phthalates-T1");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-ExposureT1");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-PD-Covars");
        csv.add("STD-2016-1449,http://hadatac.org/kb/hhear#DA-2016-1449-Lab-SpecGrav-T1");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-Phthalates");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-PD-DemoHealth");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-Smoke");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-PAH");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-VOC");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-Creatinine");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-Oxid");
        csv.add("STD-2016-1450,http://hadatac.org/kb/hhear#DA-2016-1450-Lab-Inflam");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-Lab-UEP");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-Lab-Phthalates");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-PD-DemoHealth");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-Lab-PFC");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-Lab-Pesticides");
        csv.add("STD-2016-1461,http://hadatac.org/kb/hhear#DA-2016-1461-Lab-Metals");
        csv.add("STD-2016-1523,http://hadatac.org/kb/hhear#DA-2016-1523-PD-DemoHealth");
        csv.add("STD-2016-1523,http://hadatac.org/kb/hhear#DA-2016-1523-Lab-USMOKE");
        csv.add("STD-2016-1523,http://hadatac.org/kb/hhear#DA-2016-1523-Lab-CRE");
        csv.add("STD-2016-1534,http://hadatac.org/kb/hhear#DA-2016-1534-Lab-UDILUTE");
        csv.add("STD-2016-1534,http://hadatac.org/kb/hhear#DA-2016-1534-Lab-UPHTH");
        csv.add("STD-2016-1534,http://hadatac.org/kb/hhear#DA-2016-1534-PD-DemoHealth");
        csv.add("STD-2016-34,http://hadatac.org/kb/hhear#DA-2016-34-Lab-Metals");
        csv.add("STD-2016-34,http://hadatac.org/kb/hhear#DA-2016-34-PD-DemoHealth");
        csv.add("STD-2016-34,http://hadatac.org/kb/hhear#DA-2016-34-Lab-Creatinine");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UOPFR");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-TRACE");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-HG");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UPEST");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-PD-DemoHealth");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UDILUTE");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UPAH");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UPHTH");
        csv.add("STD-2017-1598,http://hadatac.org/kb/hhear#DA-2017-1598-Lab-UPHENOL-UPB");
        csv.add("STD-2017-1740,http://hadatac.org/kb/hhear#DA-2017-1740-Lab-Metals");
        csv.add("STD-2017-1740,http://hadatac.org/kb/hhear#DA-2017-1740-Lab-Mercury");
        csv.add("STD-2017-1740,http://hadatac.org/kb/hhear#DA-2017-1740-PD-DemoHealth");
        csv.add("STD-2017-1762,http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotB");
        csv.add("STD-2017-1762,http://hadatac.org/kb/hhear#DA-2017-1762-Lab-CotA");
        csv.add("STD-2017-1762,http://hadatac.org/kb/hhear#DA-2017-1762-PD-DemoHealth");
        csv.add("STD-2017-1762,http://hadatac.org/kb/hhear#DA-2017-1762-Lab-Inflam");
        csv.add("STD-2017-1945,http://hadatac.org/kb/hhear#DA-2017-1945-Lab-Metals");
        csv.add("STD-2017-1945,http://hadatac.org/kb/hhear#DA-2017-1945-PD-DemoHealth");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-Lab-UPHEN");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-PD-DemoHealth");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-Lab-UPHTH");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-Lab-UTE");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-PD-EDCs");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-Lab-UPAH");
        csv.add("STD-2017-1977,http://hadatac.org/kb/hhear#DA-2017-1977-Lab-CRE");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-PD-FirstVoid");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-PD-DemoHealth");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Phthalates");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPAH");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_UPMs");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Oxid");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Creatinine");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPHENOL_UPB");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-UPEST_DAPs");
        csv.add("STD-2017-2121,http://hadatac.org/kb/hhear#DA-2017-2121-Lab-Inflam");
        csv.add("STD-2018-2120,http://hadatac.org/kb/hhear#DA-2018-2120-Lab-USMOKE_Plasma");
        csv.add("STD-2018-2120,http://hadatac.org/kb/hhear#DA-2018-2120-Lab-Phthalates");
        csv.add("STD-2018-2120,http://hadatac.org/kb/hhear#DA-2018-2120-Lab-Creatinine");
        csv.add("STD-2018-2120,http://hadatac.org/kb/hhear#DA-2018-2120-PD-DemoHealth");
        csv.add("STD-2018-2120,http://hadatac.org/kb/hhear#DA-2018-2120-Lab-USMOKE_Urine");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-Lab-UDILUTE");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-Lab-UTE");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-PD-DemoHealth");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-Lab-SPFAS");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-Lab-UPHTH");
        csv.add("STD-2018-2273,http://hadatac.org/kb/hhear#DA-2018-2273-Lab-UPHEN-UPB");
        csv.add("STD-2018-2517,http://hadatac.org/kb/hhear#DA-2018-2517-PD-SES");
        csv.add("STD-2018-2517,http://hadatac.org/kb/hhear#DA-2018-2517-PD-DemoHealth");
        csv.add("STD-2018-2517,http://hadatac.org/kb/hhear#DA-2018-2517-Lab-UTE");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-Lab-UEP");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-Lab-SG");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-Lab-PHTH");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-Lab-UM1");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-Lab-PAH");
        csv.add("STD-2018-2532,http://hadatac.org/kb/hhear#DA-2018-2532-PD-DemoHealth");
        csv.add("STD-2018-2537,http://hadatac.org/kb/hhear#DA-2018-2537-Lab-SG");
        csv.add("STD-2018-2537,http://hadatac.org/kb/hhear#DA-2018-2537-Lab-PHTH");
        csv.add("STD-2018-2537,http://hadatac.org/kb/hhear#DA-2018-2537-PD-DemoHealth");
        csv.add("STD-2018-2539,http://hadatac.org/kb/hhear#DA-2018-2539-PD-DemoHealth");
        csv.add("STD-2018-2539,http://hadatac.org/kb/hhear#DA-2018-2539-Lab-SPFAS");

        return csv;
    }

    private class FacetS {
        private String id;
        private String study_uri_str;
        private List<DAChild> children;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStudy_uri_str() {
            return study_uri_str;
        }

        public void setStudy_uri_str(String study_uri_str) {
            this.study_uri_str = study_uri_str;
        }

        public List<DAChild> getChildren() {
            return children;
        }

        public void setChildren(List<DAChild> children) {
            this.children = children;
        }
    }

    private class DAChild {
        private String id;
        private String acquisition_uri_str;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAcquisition_uri_str() {
            return acquisition_uri_str;
        }

        public void setAcquisition_uri_str(String acquisition_uri_str) {
            this.acquisition_uri_str = acquisition_uri_str;
        }
    }
}