package dk.kb.netarchivesuite.solrwayback.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview;
import dk.kb.netarchivesuite.solrwayback.service.dto.UrlWrapper;
import dk.kb.netarchivesuite.solrwayback.service.exception.InternalServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;

import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

@Path("/frontend/")
public class SolrWaybackResourceWeb {

    

    private static final Logger log = LoggerFactory.getLogger(SolrWaybackResourceWeb.class);

    @GET
    @Path("test")
    @Produces({ MediaType.TEXT_PLAIN})
    public String test() throws SolrWaybackServiceException {
        return "TEST";
    }
   
    @GET
    @Path("graph/domain_result")
    @Produces({ MediaType.TEXT_PLAIN})
    public String domainResultGraph(@QueryParam("q") String q, @QueryParam("fq") List<String> fq ) throws SolrWaybackServiceException {

     try {
       return Facade.generateDomainResultGraph(q,fq);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }    
     
    }
        
    
    @GET
    @Path("/pagepreviews")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ArrayList<PagePreview> search(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try {                    
        return Facade.getPagePreviewsForUrl(url);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }


    @GET
    @Path("/help/about")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAboutText() throws SolrWaybackServiceException {
      try {                    
        return Facade.getAboutText();
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
      

    @GET
    @Path("/help/search")
    @Produces( MediaType.TEXT_PLAIN)
    public String getHelpText() throws SolrWaybackServiceException {
      try {                    
        return Facade.getSearchHelpText();
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    
    @GET
    @Path("/harvestDates")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public HarvestDates harvestDates(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try {                    
        return Facade.getHarvestTimesForUrl(url);
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    @GET
    @Path("/images/search")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public  ArrayList<ImageUrl> imagesSearch(@QueryParam("query") String query) throws SolrWaybackServiceException {
      try {                                          
        ArrayList<ArcEntryDescriptor> img = Facade.findImages(query);
        return Facade.arcEntrys2Images(img);                                                            
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }
    
    
    @GET
    @Path("/wordcloud/domain")
    @Produces("image/png")
    public Response  wordCloudForDomain(@QueryParam("domain") String domain) throws SolrWaybackServiceException {
      try {                        
          BufferedImage image = Facade.wordCloudForDomain(domain);           
          return convertToPng(image);
          
      } catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }

    
    
    @GET
    @Path("/util/normalizeurl")
    @Produces(MediaType.APPLICATION_JSON)
    public UrlWrapper waybackgraph(@QueryParam("url") String url) throws SolrWaybackServiceException {
      try{
       
        //also rewrite to puny code
        String url_norm =  Facade.punyCodeAndNormaliseUrl(url);       
        log.info("Normalize url"+url +" ->" +  url_norm);        
        UrlWrapper wrapper = new UrlWrapper();
        wrapper.setUrl(url_norm);      
        return wrapper;
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }
        
    
    @POST
    @Path("upload/gethash")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String uploadPdf(List<Attachment> attachments,@Context HttpServletRequest request) throws  SolrWaybackServiceException { 

        log.info("upload called");          
        if (attachments.size() != 1) {
          log.info("upload most have 1 attachments, #attachments="+attachments.size());
         throw new InvalidArgumentServiceException("Only 1 attachment allowed. #attachments="+attachments.size());   
        }      
     try {                             
            Attachment attr= attachments.get(0);
            DataHandler handler = attr.getDataHandler();     
            InputStream uploadedInputStream = handler.getInputStream();
            String sha1 = Sha1Hash.createSha1(uploadedInputStream);               
            return sha1;

      } catch (Exception e) {         
        throw handleServiceExceptions(e);
      }      
    }
    
    //No facets! Only results
    @GET
    @Path("solr/search/results") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchResults(@QueryParam("query") String query, @QueryParam("fq") List<String> fq ,  @QueryParam("grouping") boolean grouping,  @QueryParam("revisits") boolean revisits , @QueryParam("start") int start) throws SolrWaybackServiceException {
      try {
        String res = Facade.solrSearchNoFacets(query,fq, grouping, revisits, start);          
        return res;
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }
    
    
    //No results, only facets
    @GET
    @Path("solr/search/facets") 
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearchFacets(@QueryParam("query") String query, @QueryParam("fq") List<String> fg , @QueryParam("revisits") boolean revisits) throws SolrWaybackServiceException {
      try {
       String res = Facade.solrSearchFacetsOnly(query,fg, revisits);          
       return res;
      } catch (Exception e) {        
        throw handleServiceExceptions(e);
      }
    }
    
    
    
    
    @GET
    @Path("solr/idlookup")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public String  solrSearch(@QueryParam("id") String id) throws SolrWaybackServiceException {
      try {                    
       
         String res = Facade.solrIdLookup(id);          
        return res;
      } catch (Exception e) {
        log.error("error id lookup:"+id, e);
        throw handleServiceExceptions(e);
      }
    }
    

    @GET
    @Path("properties/solrwaybackweb")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public HashMap<String,String>  getPropertiesWeb() throws SolrWaybackServiceException {
      try {                    
        log.info("PropertiesWeb returned");
        return Facade.getPropertiesWeb();          
      } catch (Exception e) {
        throw handleServiceExceptions(e);
      }
    }

    

    //TODO want to remove this method from web frontend
    @GET
    @Path("/downloadRaw")
    public Response downloadRaw(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset) throws SolrWaybackServiceException {
      try {

        log.debug("Download from FilePath:" + source_file_path + " offset:" + offset);
        ArcEntry arcEntry= Facade.getArcEntry(source_file_path, offset);
        
        //Only solr lookup if redirect.
        if (arcEntry.getStatus_code() >= 300 &&  arcEntry.getStatus_code() <= 399 ){
          IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);         
          Response responseRedirect = SolrWaybackResource.getRedirect(indexDoc,arcEntry);
          log.debug("Redirecting. status code from arc:"+arcEntry.getStatus_code() + " vs index " +indexDoc.getStatusCode()); 
          return responseRedirect;
        }
        
        //temp dirty hack to see if it fixes brotli
        InputStream in;
        if ("br".equalsIgnoreCase(arcEntry.getContentEncoding())){
        in = new BrotliInputStream(new ByteArrayInputStream(arcEntry.getBinary()));
        arcEntry.setContentEncoding(null); //Clear encoding.
        arcEntry.setHasBeenDecompressed(true);
        }
        else{      
         in = new ByteArrayInputStream(arcEntry.getBinary());
         
        }
        ResponseBuilder response = null;
        try{
          String contentType = arcEntry.getContentType();
          if (arcEntry.getContentCharset() != null){
            contentType = contentType +"; charset="+arcEntry.getContentCharset();
          }        
          response= Response.ok((Object) in).type(contentType);          
        }
        catch (Exception e){         
          IndexDoc indexDoc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); 
           log.warn("Error setting HTTP header Content-Type:'"+arcEntry.getContentType() +"' using index Content-Type:'"+indexDoc.getContentType()+"'");         
           response = Response.ok((Object) in).type(indexDoc.getContentType()); 
        }
              
        if (arcEntry.getFileName() != null){
          response.header("Content-Disposition", "filename=\"" + arcEntry.getFileName() +"\"");      
        }
        
        if (arcEntry.getContentEncoding() != null){
          response.header("Content-Encoding", arcEntry.getContentEncoding());      
        }
        
        log.debug("Download from source_file_path:" + source_file_path + " offset:" + offset + " is mimetype:" + arcEntry.getContentType() + " and has filename:" + arcEntry.getFileName());
        return response.build();

      } catch (Exception e) {
        log.error("Error download from source_file_path:"+ source_file_path + " offset:" + offset,e);
        throw handleServiceExceptions(e);
      }
    }

    
    @GET
    @Path("images/htmlpage")
    @Produces(MediaType.APPLICATION_JSON +"; charset=UTF-8")
    public ArrayList<ImageUrl> imagesForPage(@QueryParam("source_file_path") String source_file_path, @QueryParam("offset") long offset ) throws SolrWaybackServiceException {

      if (source_file_path == null || offset < 0){
        log.error("source_file_path and offset queryparams missing");
        throw new InvalidArgumentServiceException("source_file_path and offset queryparams missing");
      }

      try {    
        ArrayList<ImageUrl> images = Facade.getImagesForHtmlPageNew(source_file_path, offset);
        return images;     
      }
      catch (Exception e) {           
        throw handleServiceExceptions(e);
      }
    }

    
    
    private Response convertToPng(BufferedImage image)  throws Exception { 
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      byte[] imageData = baos.toByteArray();
      baos.flush();
      baos.close();
      ResponseBuilder response = Response.ok(new ByteArrayInputStream(imageData));
      return response.build();
    }

    
    private SolrWaybackServiceException handleServiceExceptions(Exception e) {
        if (e instanceof SolrWaybackServiceException) {
          log.info("Handling serviceException:" + e.getMessage());
          return (SolrWaybackServiceException) e; // Do nothing, exception already correct
        } else if (e instanceof IllegalArgumentException) {
          log.error("ServiceException(HTTP 400) in Service:", e.getMessage());
          return new InvalidArgumentServiceException(e.getMessage());
        } else {// unforseen exceptions.... should not happen.
          log.error("ServiceException(HTTP 500) in Service:", e);
          return new InternalServiceException(e.getMessage());
        }
      }
    
    
}
