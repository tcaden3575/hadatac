package org.hadatac.console.controllers.studies;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.typesafe.config.ConfigFactory;
import org.hadatac.Constants;
import org.hadatac.console.controllers.Application;
import org.hadatac.console.providers.MyUsernamePasswordAuthProvider;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import org.hadatac.console.views.html.studies.*;
import org.hadatac.entity.pojo.Measurement;
import org.hadatac.entity.pojo.ObjectCollection;
import org.hadatac.entity.pojo.Study;
import org.hadatac.entity.pojo.Agent;
import org.hadatac.entity.pojo.StudyObject;
import org.hadatac.metadata.loader.URIUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.hadatac.console.controllers.AuthApplication;
import org.hadatac.console.controllers.objectcollections.OCForceFieldGraph;
import org.hadatac.console.controllers.triplestore.UserManagement;
import org.hadatac.console.http.SPARQLUtils;
import org.hadatac.console.models.SysUser;
import org.hadatac.utils.CollectionUtil;
import org.hadatac.utils.ConfigProp;
import org.hadatac.utils.NameSpaces;

import be.objectify.deadbolt.java.actions.Group;
import be.objectify.deadbolt.java.actions.Restrict;

import javax.inject.Inject;

public class StudyView extends Controller {

    @Inject
    Application application;
    public static int PAGESIZE = 7;

    //@Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result index(String study_uri, String oc_uri, int page, Http.Request request) {
        System.out.println("Inside StudyView @Secure(authorizers = Constants.DATA_OWNER_ROLE): [" + study_uri + "]");
        if (oc_uri != null && oc_uri.indexOf("STD-") > -1) {
            oc_uri = "";
        }
        //System.out.println("Study URI: [" + study_uri + "]");
        //System.out.println("SOC URI: [" + oc_uri + "]");

        try {
            study_uri = URLDecoder.decode(study_uri, "utf-8");
            oc_uri = URLDecoder.decode(oc_uri, "utf-8");
        } catch (UnsupportedEncodingException e) {
            study_uri = "";
            oc_uri = "";
        }

        System.out.println("StudyUri: " + study_uri);

        OCForceFieldGraph graph = new OCForceFieldGraph(OCForceFieldGraph.NO_TIME_SPACE, study_uri);

        if (study_uri == null || study_uri.equals("")) {
            return badRequest("ViewStudy: [ERROR] empty study URI");
        }
        Study study = Study.find(study_uri);
        if (study == null) {
            return badRequest("ViewStudy: [ERROR] Could not find any study with following URI: [" + study_uri + "]");
        }
       Agent agent = study.getAgent();
        Agent institution = study.getInstitution();

        //using config
        String fullImgPrefix = ConfigFactory.load().getString("hadatac.shiny_dashboards.fullImgPrefix");
        StringBuilder shinyAppUrl = new StringBuilder();
        shinyAppUrl.append(fullImgPrefix);

        //to add the Study
        shinyAppUrl.append(study.getId());

        System.out.println("shinyAppUrl: [" + shinyAppUrl.toString() + "]");

        String source = "studypage";
        String studyIds = "";
        Map<String, String[]> params = getRequestParameters( request);
        System.out.println("params: "+ params);
        if(params.containsKey("source")) {
            System.out.println("params.get(source): "+ params.get("source"));
            source = params.get("source")[0];
        }
        if(params.containsKey("studyIds")) {
            System.out.println("params.get(studyIds): "+ params.get("studyIds"));
            studyIds = params.get("studyIds")[0];
            System.out.println("params.get(studyIds)[0]): "+ studyIds);
        }


        ObjectCollection oc = null;
        if (oc_uri != null && !oc_uri.equals("")) {
            oc = ObjectCollection.find(oc_uri);
        }

        List<StudyObject> objects = null;
        int total = 0;
        if (oc != null) {
            objects = StudyObject.findByCollectionWithPages(oc, PAGESIZE, page * PAGESIZE);
            total = StudyObject.getNumberStudyObjectsByCollection(oc_uri);
        }

        return ok(studyView.render(graph.getTreeQueryResult().replace("\n", " "),
                study, agent, institution, oc, objects, page, total,application.getUserEmail(request),
                source, studyIds, shinyAppUrl.toString()));
    }

    //@Secure(authorizers = Constants.DATA_OWNER_ROLE)
    public Result postIndex(String study_uri, String oc_uri, int page, Http.Request request) {
        return index(study_uri, oc_uri, page, request);
    }

    public Map<String, String[]> getRequestParameters(Http.Request request) {
        final Map<String, String[]> parameters = new HashMap<>();
        final Map<String, String[]> urlParameters = request.queryString();
        if (urlParameters != null) {
            parameters.putAll(urlParameters);
        }
        return parameters;
    }

}
