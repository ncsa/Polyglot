package edu.illinois.ncsa.isda.softwareserver;
import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/")
public interface SoftwareServerRESTEasyInterface
{
	/**
	 * Method:   Welcome.
	 * @returns: SoftwareServer greetings
	 */
	@GET
	@Produces("text/html,text/plain")
	public Response WelcomeToSoftwareServer();

	/**
	 * Method: listApplications.
	 * Returns a list of applications available in software server
	 * @param uriInfo Basic URL information
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
	 * Returns a list of tasks performed by a particular application
	 * @param uriInfo Basic URL information
	 * @param produces content type accepted by client
	 * @param app application's name
	 * @return list of tasks performed by a particular application
	 */
	@GET
	@Path("software/{app}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listTasks(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces, @PathParam("app") String app);

	/**
	 * Method: listOutputFmts.
	 * Return a list of the file formats produced by the application
	 * @param uriInfo: Basic URL information
	 * @param produces content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of the file formats produced by the application
	 */
	@GET
	@Path("software/{app}/{tsk}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listOutputFmts(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listInputAndOutputFormats.
	 * Returns a list of input and output file formats accepted and produced by the application
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of input and output file formats accepted and produced by the application
	 */
	@GET
	@Path("software/{app}/{tsk}/*")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response listInputAndOutputFormats(@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listInputFormats.
	 * Returns a list of input file formats accepted by the application
	 * @param uriInfo Basic URL information
	 * @param produces content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @return list of input file formats accepted by the application
	 */
	@GET
	@Path("software/{app}/{tsk}/{fmt: [^*]+}")
	@Consumes("text/html,text/plain")
	@Produces("text/html,text/plain")
	public Response listInputFormats(@Context UriInfo uriInfo, @HeaderParam("Accept") String produces,
			@PathParam("app") String app, @PathParam("tsk") String tsk);

	/**
	 * Method: listOutputFile.
	 * Returns a link to the produced file
	 * @param uriInfo Basic URL information
	 * @param produces content type accepted by client
	 * @param app application's name
	 * @param tsk task to be performed
	 * @param fmt requested output format
	 * @param file input file to be processed 
	 * @return link to the produced file
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
	 * Returns a list of actions that can be performed (get or post or convert) 
	 * @return list of actions that can be performed
	 */
	@GET
	@Path("/form")
	@Consumes("text/html,text/plain")
	@Produces("text/plain")
	public Response form();

	/**
	 * Method: processGet.
	 * Returns a link to the produced file
	 * @param uriInfo Basic URL information
	 * @param app application's name
	 * @param tsk task to be performed
	 * @param fmt requested output format
	 * @param file input file to be processed 
	 * @param action (get or post or convert)
	 * @return a link to the produced file
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
	 * Returns a link to either a produced file or to a directory tree
	 * @param uriInfo Basic URL information
	 * @param fileOrDirName input file or directory
	 * @return a link to either a produced file or to a directory tree
	 */
	@GET
	@Path("/file/{name: .+}")
	@Consumes("text/*")
	@Produces("multipart/all")
	public Response fileOrDir(@Context UriInfo uri, @PathParam("name") String fileOrDirName) throws IOException;

	/**
	 * Method: appIcons.
	 * Returns the requested icon file if available. If not, a default icon file is return instead.
	 * @param fileName file name of icon
	 * @return the requested icon file if available, default icon icon if not
	 */
	@GET
	@Path("/image/{icon}")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response appIcons(@PathParam("icon") String fileName);

	/**
	 * Method: alive.
	 * Used to verify if the server is responding
	 * @return yes if the server is alive 
	 */
	@GET
	@Path("/alive")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response alive();

	/**
	 * Method: busy.
	 * Used to verify if the server is busy
	 * @return true if the server is processing at least one request, false if not. 
	 */
	@GET
	@Path("/busy")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response busy();

	/**
	 * Method: processors.
	 * Used to know the number of processor available to the server 
	 * @return number of processors available to the server 
	 */
	@GET
	@Path("/processors")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response processors();

	/**
	 * Method: memory.
	 * Used to know the memory available to the server 
	 * @return memory available to the server (is this right????)  
	 */
	@GET
	@Path("/memory")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response memory();

	/**
	 * Method: load.
	 * Used to get a measure of the server load
	 * @return measure of the server load  
	 */
	@GET
	@Path("/load")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response load();

	/**
	 * Method: tasks.
	 * Used to know the number of task processed since the server starts
	 * @return number of task since the server starts
	 */
	@GET
	@Path("/tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response tasks();

	/**
	 * Method: kills.
	 * Used to know the number of task killed since the server starts
	 * @return number of task killed since the server starts
	 */
	@GET
	@Path("/kills")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response kills();

	/**
	 * Method: completedTasks.
	 * Used to know the number of task completed since the server starts
	 * @return number of task completed since the server starts
	 */
	@GET
	@Path("/completed_tasks")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response completedTasks();

	/**
	 * Method: screen.
	 * Used to have a look at the server screen and determine if the
	 * server must be restarted due to hang application.
	 * @return number an image of the server screen (only authorized users)
	 */
	@GET
	@Path("/screen")
	@Consumes("text/*")
	@Produces("image/jpeg")
	public Response screen();

	/**
	 * Method: reset.
	 * Used to reset the server remotely in case it is necessary
	 * @return ok after reseting the server. (only authorized users)
	 */
	@GET
	@Path("/reset")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reset();

	/**
	 * Method: reboot.
	 * Used to reboot the server remotely in case it is necessary
	 * @return ok after rebooting the server. (only authorized users)
	 */
	@GET
	@Path("/reboot")
	@Consumes("text/*")
	@Produces("text/plain")
	public Response reboot();

	/**
	 * Method: printErrorMessage.
	 * Returns an error message 
	 * @param msg String to be returned within the error message
	 * @return error message
	 */
	@GET
	@Path("/{param}")
	@Produces("text/plain")
	public Response printErrorMessage(@PathParam("param") String msg);


	/**
	 * Method: formPost.
	 * Returns a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uriInfo Basic URL information
	 * @param app application's name
	 * @param tsk task to be performed
	 * @param fmt requested output format
	 * @param file name of the input file to be processed 
	 * @return a link to the produced file
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
	 * Returns a link to the produced file
	 * @param input Form containing Input Data
	 * @param mediaType media type being processed 
	 * @param accept content type accepted by client
	 * @param uriInfo Basic URL information
	 * @param app application's name
	 * @param tsk task to be performed
	 * @param fmt requested output format
	 * @return a link to the produced file
	 */
	@POST
	@Path("/{softOrForm}/{app}/{tsk}/{fmt}")
	// @Consumes("multipart/form-data")
	@Produces("text/plain")
	public Response taskPost(MultipartFormDataInput input, @HeaderParam("Content-Type") String mediaType,
			@HeaderParam("Accept") String accept, @Context UriInfo uri, @PathParam("app") String app,
			@PathParam("tsk") String tsk, @PathParam("fmt") String fmt);
}
