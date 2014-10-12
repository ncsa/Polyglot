package edu.illinois.ncsa.isda.softwareserver;
import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/")
public interface SoftwareServerRESTEasyInterface
{
	@GET
	@Produces("text/html,text/plain")
	public Response WelcomeToSoftwareServer();

	@GET
	@Path("software")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listApplications(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces);

	@GET
	@Path("software/{app}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listTasks(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces, @PathParam("app") String app);

	@GET
	@Path("software/{app}/{tsk}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listOutputFmts(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	@GET
	@Path("software/{app}/{tsk}/*")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response listInputAndOutputFormats(@PathParam("app") String app, @PathParam("tsk") String tsk);

	@GET
	@Path("software/{app}/{tsk}/{fmt: [^*]+}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listInputFormats(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	@GET
	@Path("software/{app}/{tsk}/{fmt}/{file}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listOutputFile(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk, @PathParam("fmt") String fmt,
			@PathParam("file") String file);

	@GET
	@Path("/form")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response form();

	@GET
	@Path("/form/{action}")
	@Consumes("text/html,text/plain")
	@Produces("text/html")
	public Response processGet(@Context UriInfo uri, @DefaultValue("") @QueryParam("application") String app,
			@DefaultValue("") @QueryParam("task") String tsk, @DefaultValue("") @QueryParam("format") String fmt,
			@DefaultValue("") @QueryParam("file") String file, @PathParam("action") String action);

	@GET
	@Path("/file/{name: .+}")
	@Consumes("text/*")
	@Produces("multipart/all")
	public Response fileOrDir(@Context UriInfo uri, @PathParam("name") String fileOrDirName) throws IOException;

	@GET
	@Path("/image/{icon}")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response appIcons(@PathParam("icon") String fileName);

	@GET
	@Path("/alive")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response alive();

	@GET
	@Path("/busy")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response busy();

	@GET
	@Path("/processors")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response processors();

	@GET
	@Path("/memory")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response memory();

	@GET
	@Path("/load")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response load();

	@GET
	@Path("/tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response tasks();

	@GET
	@Path("/kills")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response kills();

	@GET
	@Path("/completed_tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response completedTasks();

	@GET
	@Path("/screen")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response screen();

	@GET
	@Path("/reset")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reset();

	@GET
	@Path("/reboot")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reboot();

	@GET
	@Path("/{param}")
	@Produces("text/plain")
	public Response printErrorMessage(@PathParam("param") String msg);

	@POST
	@Path("/form/{action}")
	@Consumes("multipart/form-data")
	@Produces("text/html, text/plain")
	public Response formPost(MultipartFormDataInput input, @HeaderParam("Content-Type") String mediaType,
			@HeaderParam("Accept") String accept, @Context UriInfo uri, @DefaultValue("") @QueryParam("application") String app,
			@DefaultValue("") @QueryParam("task") String tsk, @DefaultValue("") @QueryParam("format") String fmt,
			@DefaultValue("") @QueryParam("file") String file);

	@POST
	@Path("/{softOrForm}/{app}/{tsk}/{fmt}")
	// @Consumes("multipart/form-data")
	@Produces("text/plain")
	public Response taskPost(MultipartFormDataInput input, @HeaderParam("Content-Type") String mediaType,
			@HeaderParam("Accept") String accept, @Context UriInfo uri, @PathParam("app") String app,
			@PathParam("tsk") String tsk, @PathParam("fmt") String fmt);
}
