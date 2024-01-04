package org.hadatac.console.controllers;

import org.hadatac.console.views.html.landingPage;
import org.pac4j.play.PlayWebContext;
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

        System.out.println("Portal:index->application.getUserEmail(request)"+application.getUserEmail(request)+"\n\n");
        SysUser user = AuthApplication.getAuthApplication().getUserProvider().getUser(application.getUserEmail(request));

        //final PlayWebContext context = new PlayWebContext(request, playSessionStore);
        //System.out.println("SignUp:createUserProfile->getSessionId():"+application.getPlayWebContext().getSessionStore().getOrCreateSessionId(playWebContext)+"\n\n");
        //System.out.println("SignUp:createUserProfile->getSessionId():"+context.getSessionStore().getOrCreateSessionId(playWebContext)+"\n\n");

        System.out.println("Portal:index->SysUser user is = "+user+"\n\n");

        if (user == null) {
            System.out.println("Portal:index->user is null = "+user+"\n\n");
            System.out.println("Portal:index->application.getUserEmail(request)"+application.getUserEmail(request)+"\n\n");
            return ok(landingPage.render(application.getUserEmail(request)));
        } else {
            System.out.println("Portal:index->user is not null = "+user.getEmail()+"\n\n");
            System.out.println("Portal:index->application.getUserEmail(request)"+application.getUserEmail(request)+"\n\n");
            return ok(portal.render(application.getUserEmail(request)));
        }
    }

    public Result postIndex(Http.Request request){
        return ok(portal.render(application.getUserEmail(request)));}
}