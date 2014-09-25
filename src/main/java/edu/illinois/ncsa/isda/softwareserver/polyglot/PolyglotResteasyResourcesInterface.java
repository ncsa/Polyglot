package edu.illinois.ncsa.isda.softwareserver.polyglot;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/")
public interface PolyglotResteasyResourcesInterface {

  @GET
  @Consumes("text/html,text/plain")
  @Produces("text/html,text/plain")
  public Response Endpoints(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces);

  @GET
  @Path("convert")
  @Consumes("text/html,text/plain")
  @Produces("text/html,text/plain")
  public Response Convert(@Context UriInfo uriInfo, @HeaderParam("Accept") String accept);

  @GET
  @Path("convert/{outFmt}")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response ConvertFmt(@PathParam("outFmt") String outFmt);
  
  @GET
  @Path("convert/{outFmt}/{file : .+}")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response ConvertFile(@Context UriInfo uriInfo, 
  		                        @HeaderParam("Accept") String accept, 
  		                        @Context HttpServletRequest req,  
  		                        @PathParam("outFmt") String outFmt, 
  		                        @PathParam("file") String file) throws IOException;
  
  
  @GET
  @Path("form")
  @Consumes("text/html,text/plain")
  @Produces("text/html,text/plain")
  public Response Form(@Context UriInfo uriInfo, @HeaderParam("Accept") String accept);
  
  
  @GET
  @Path("form/{action}")
  @Consumes("text/html,text/plain")
  @Produces("text/html,text/plain")
  public Response Forms(@Context UriInfo uriInfo, 
      									@DefaultValue("") @QueryParam("output") String output,
      									@DefaultValue("") @QueryParam("file") 	String file,
      									@PathParam("action")  									String action);
		
  @GET
  @Path("/file/{name: .+}")
  @Consumes("text/*")
  @Produces("multipart/all")
  public Response File(@Context UriInfo uri, @PathParam("name") String fileName) throws IOException ;
 
  
  @GET
  @Path("/image/{icon}")
  @Consumes("text/*")
  @Produces("image/jpeg")
  public Response appIcons(@PathParam("icon") String fileName);
 
  
  @GET
  @Path("alive")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response Alive();
  
  @GET
  @Path("servers")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response Servers();

  @GET
  @Path("software")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response Software();

  @GET
  @Path("inputs")
  @Consumes("text/html,text/plain")
  @Produces("text/html,text/plain")
  public Response Inputs(@Context UriInfo uriInfo, @HeaderParam("Accept") String accept);

  @GET
  @Path("inputs/{out  : .+}")
  public Response InputsOut(@Context UriInfo uriInfo, @HeaderParam("Accept") String accept, @PathParam("out") String out);
  
  @GET
  @Path("outputs")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response Outputs();
  
  @GET
  @Path("requests")
  @Consumes("text/html,text/plain")
  @Produces("text/plain")
  public Response Requests();
  
  @GET
  @Path("/{param}")
  @Produces("text/plain")
  public Response printErrorMessage(@PathParam("param") String msg);

  
  
  @POST
  @Path("/form/{action}")
  @Consumes("multipart/form-data")
  @Produces("text/html, text/plain")
  public Response formPost(MultipartFormDataInput input, 
  												@HeaderParam("Content-Type")                    String mediaType,
  												@HeaderParam("Accept")                 	        String accept,
  												@Context 										UriInfo uri,
  												@Context HttpServletRequest req,
  												@DefaultValue("") @QueryParam("output") 			String output);

  
  @POST
  @Path("/{softOrForm}/{output}")
  //@Consumes("multipart/form-data")
  @Produces("text/plain")
  public Response taskPost(MultipartFormDataInput input, 
  												@HeaderParam("Content-Type") String mediaType,
  												@HeaderParam("Accept")       String accept,
  												@Context 				     UriInfo uri,
  												@Context HttpServletRequest req,
  												@PathParam("output") 		     String output) ;
  
  
} // end of class PolyglotResteasyResources //
