package edu.illinois.ncsa.isda.softwareserver;
import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/")
public interface SoftwareServerRESTEasyInterface
{
	/**
	 * Welcome method.
	 * @return SoftwareServer greetings
	 */
	@GET
	@Produces("text/html,text/plain")
   public Response WelcomeToSoftwareServer();

	/**
	 * Method: listApplications.
	 * @param uriInfo Basic information about request URL
	 * @param produces content type accepted by client
	 * @return list of applications available in software server
	 */
	@GET
	@Path("software")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listApplications(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces);

	/**
	 * Method: listTasks.
	 * @param uriInfo Basic information about request URL
	 * @param produces content type accepted by client
	 * @param app application
	 * @return list of tasks related to application
	 */
	@GET
	@Path("software/{app}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listTasks(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces, @PathParam("app") String app);

	/**
	 * Method: listOutputFmts.
	 * @param uriInfo  URL information
	 * @param produces content type accepted by client
	 * @param app application
	 * @param tsk requested task
	 * @return list of output formats available per application per task
	 */
	@GET
	@Path("software/{app}/{tsk}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listOutputFmts(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listInputAndOutputFormats.
	 * @param app application
	 * @param tsk requested task
	 * @return list of input and output formats available per application per task
	 */
	@GET
	@Path("software/{app}/{tsk}/*")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response listInputAndOutputFormats(@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listInputFormats.
	 * @param uriInfo  URL information
	 * @param produces content type accepted by client
	 * @param app application
	 * @param tsk requested task
	 * @return list of input formats available per application per task
	 */
	@GET
	@Path("software/{app}/{tsk}/{fmt: [^*]+}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listInputFormats(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listOutputFile.
	 * @param uriInfo  URL information
	 * @param produces content type accepted by client
	 * @param app application
	 * @param tsk requested task
	 * @param fmt requested output format
	 * @param file input file 
	 * @return a link to the converted file
	 */
	@GET
	@Path("software/{app}/{tsk}/{fmt}/{file}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listOutputFile(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk, @PathParam("fmt") String fmt,
			@PathParam("file") String file);

	/**
	 * Method: form.
	 * @return list of actions (get or post or convert)
	 */
	@GET
	@Path("/form")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response form();

	/**
	 * processGet method.
	 * @param uri  URI information
	 * @param app application
	 * @param tsk requested task
	 * @param fmt requested output format
	 * @param file input file 
	 * @param action (get or post or convert)
	 * @return a link to the converted file
	 */
	@GET
	@Path("/form/{action}")
	@Consumes("text/html,text/plain")
	@Produces("text/html")
	public Response processGet(@Context UriInfo uri, @DefaultValue("") @QueryParam("application") String app,
			@DefaultValue("") @QueryParam("task") String tsk, @DefaultValue("") @QueryParam("format") String fmt,
			@DefaultValue("") @QueryParam("file") String file, @PathParam("action") String action);

	/**
	 * Method: fileOrDir.
	 * @param uri  URI information
	 * @param fileOrDirName input file or directory
	 * @return a link to the converted file or link to directory
	 */
	@GET
	@Path("/file/{name: .+}")
	@Consumes("text/*")
	@Produces("multipart/all")
	public Response fileOrDir(@Context UriInfo uri, @PathParam("name") String fileOrDirName) throws IOException;

	/**
	 * Method: appIcons.
	 * @param fileName icon file name
	 * @return the requested icon
	 */
	@GET
	@Path("/image/{icon}")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response appIcons(@PathParam("icon") String fileName);

	/**
	 * Method: alive.
	 * @return yes if the server is alive 
	 */
  @GET
	@Path("/alive")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response alive();

	/**
	 * busy method.
	 * @return true if the server is processing a request, false if not. 
	 */
	@GET
	@Path("/busy")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response busy();

	/**
	 * Method: processors.
	 * @return number of processors available to the server 
	 */
	@GET
	@Path("/processors")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response processors();

	/**
	 * Method: memory.
	 * @return memory available to the server (is this right????)  
	 */
	@GET
	@Path("/memory")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response memory();

	/**
	 * Method: load.
	 * @return measure of the server load  
	 */
	@GET
	@Path("/load")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response load();

	/**
	 * Method: tasks.
	 * @return number of task since the server starts
	 */
	@GET
	@Path("/tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response tasks();

	/**
	 * Method: kills.
	 * @return number of task killed since the server starts
	 */
	@GET
	@Path("/kills")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response kills();

	/**
	 * Method: completedTasks.
	 * @return number of task completed since the server starts
	 */
	@GET
	@Path("/completed_tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response completedTasks();

	/**
	 * Method: screen.
	 * @return number an image of the server screen (only authorized users)
	 */
	@GET
	@Path("/screen")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response screen();

	/**
	 * Method: reset.
	 * @return ok after reseting the server. (only authorized users)
	 */
	@GET
	@Path("/reset")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reset();

	/**
	 * Method: reboot.
	 * @return ok after rebooting the server. (only authorized users)
	 */
	@GET
	@Path("/reboot")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reboot();

	/**
	 * Method: printErrorMessage.
	 * @param msg Error message to be returned
	 * @return error message
	 */
	@GET
	@Path("/{param}")
	@Produces("text/plain")
	public Response printErrorMessage(@PathParam("param") String msg);


	/**
	 * Method: formPost.
	 * @param input MultipartFormDataInput
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri  URL information
	 * @param app application
	 * @param tsk requested task
	 * @param fmt requested output format
	 * @param file input file 
	 * @return a link to the converted file
	 */
	@POST
	@Path("/form/{action}")
	@Consumes("multipart/form-data")
	@Produces("text/html, text/plain")
	public Response formPost(MultipartFormDataInput input, @HeaderParam("Content-Type") String mediaType,
			@HeaderParam("Accept") String accept, @Context UriInfo uri, @DefaultValue("") @QueryParam("application") String app,
			@DefaultValue("") @QueryParam("task") String tsk, @DefaultValue("") @QueryParam("format") String fmt,
			@DefaultValue("") @QueryParam("file") String file);

	/**
	 * Method: taskPost.
	 * @param input MultipartFormDataInput
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uri  URL information
	 * @param app application
	 * @param tsk requested task
	 * @param fmt requested output format
	 * @param file input file 
	 * @return a link to the converted file
	 */
@POST
	@Path("/{softOrForm}/{app}/{tsk}/{fmt}")
	// @Consumes("multipart/form-data")
	@Produces("text/plain")
	public Response taskPost(MultipartFormDataInput input, @HeaderParam("Content-Type") String mediaType,
			@HeaderParam("Accept") String accept, @Context UriInfo uri, @PathParam("app") String app,
			@PathParam("tsk") String tsk, @PathParam("fmt") String fmt);
}