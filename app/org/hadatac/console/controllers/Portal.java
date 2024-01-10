package org.hadatac.console.controllers;

import org.apache.commons.lang.StringUtils;
import org.hadatac.console.views.html.landingPage;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import org.hadatac.console.views.html.main;
import org.hadatac.console.views.html.portal;
import org.hadatac.console.views.html.dashboard;
import org.hadatac.console.models.SysUser;
import org.hadatac.utils.Repository;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class Portal extends Controller {
    @Inject Application application;

    public Result index(Http.Request request) {
        if (!Repository.operational(Repository.METADATA)) {
            return ok(main.render("Results", "","",
                    new Html("<div class=\"container-fluid\"><h4>"
                            + "The triple store is NOT properly connected. "
                            + "Please restart it or check the hadatac configuration file!"
                            + "</h4></div>")));
        }
        if (!Repository.operational(Repository.DATA)) {
            return ok(main.render("Results", "","",
                    new Html("<div class=\"container-fluid\"><h4>"
                            + "HADatAC Solr is down now. Ask Administrator for further information. "
                            + "</h4></div>")));
        }
        if (!Repository.checkNamespaceWithQuads()) {
            return ok(main.render("Results", "","",
                    new Html("<div class=\"container-fluid\"><h4>"
                            + "The namespace store is set to be without quads. Ask Administrator for further information. "
                            + "</h4></div>")));
        }

        //System.out.println("Portal: Starting ");
        Optional<String> userValidatedSessionValue = getSessionParameters( request, "userValidated");
        String userValidated = (userValidatedSessionValue.isPresent()) ? userValidatedSessionValue.get() : null;
        //System.out.println("Portal:index-->session userValidated: "+ userValidated);

        Optional<String> userEmailSessionValue = getSessionParameters( request, "userEmail");
        String userEmail = (userEmailSessionValue.isPresent()) ? userEmailSessionValue.get() : null;
        //System.out.println("Portal:index-->session userEmail: "+ userEmail);

        //SysUser user = AuthApplication.getAuthApplication().getUserProvider().getUser(application.getUserEmail(request));
        SysUser user = null;

        if(userValidated!=null && userValidated.equalsIgnoreCase("yes")  && StringUtils.isNotBlank(userEmail))
        {
            user = AuthApplication.getAuthApplication().getUserProvider().getUser(userEmail);
        }
        else {
            userEmail = application.getUserEmail(request);
            user = AuthApplication.getAuthApplication().getUserProvider().getUser(userEmail);
        }
        //System.out.println("Portal:index->SysUser user is = "+user+"\n");

        if (user == null) {
            //System.out.println("Portal:index->user is null \n");
            //System.out.println("Portal:index->redirecting to Landing page \n");
            return ok(landingPage.render(userEmail));
            //return ok(landingPage.render(application.getUserEmail(request)));
        } else {
            //System.out.println("Portal:index->user is not null = "+user.getEmail()+"\n\n");
            //System.out.println("Portal:index->redirecting to Portal page \n");
            return ok(portal.render(userEmail));
            //return ok(portal.render(application.getUserEmail(request)));
        }
    }

    public Result postIndex(Http.Request request){
        return ok(portal.render(application.getUserEmail(request)));}


    public Optional<String> getSessionParameters(Http.Request request, String param) {
        return request
                .session()
                .get(param);
    }
}