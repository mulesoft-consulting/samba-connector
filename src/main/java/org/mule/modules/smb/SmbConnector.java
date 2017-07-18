package org.mule.modules.smb;

import org.slf4j.LoggerFactory;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.lifecycle.OnException;
import org.mule.api.annotations.param.Payload;
import org.mule.modules.smb.config.ConnectorConfig;
import org.mule.modules.smb.error.ErrorHandler;
import org.mule.util.IOUtils;

@Connector(name="smb", friendlyName="Smb")
@OnException(handler=ErrorHandler.class)
public class SmbConnector {

	private static Logger _logger = LoggerFactory.getLogger(SmbConnector.class);

	@Config
	ConnectorConfig config;

	
	/**
	 * This method will save a file in a file location.
	 * 
	 * @param fileName The name of the file
	 * @param data the content of the file as InputStream. If you have byte[] array, please following to convert the content as InputStream 'new ByteArrayInputStream(sBytes)'
	 * @return true or false
	 */
	@Processor 
	public boolean saveFileFromStream(String fileName, InputStream data ) {
		_logger.debug("Start->saveFile");
		
        NtlmPasswordAuthentication auth = this.getNtlmAuth();
		String path = getSambaConnectionString() + fileName;
		
		SmbFileOutputStream out = null;
		try {
			SmbFile resource = new SmbFile(path, auth);
            out = new SmbFileOutputStream(resource);
            out.write(IOUtils.toByteArray(data));
            out.flush();
            out.close();
            return true;
		} catch (Exception e) {
			_logger.error("Something went wrong while writing the file",e);	
			try {
				if(out != null)
					out.close();
			} catch (IOException ignored) {}
		}
		
		_logger.debug("End->saveFile");
		return false;
		
	}
	

	/**
	* 	This processor will return a map of fully qualified file name and its content present at the -
	* 	configured folder based on a file name pattern
	*
	*	@param payload 
	*	@param fileNamePattrn The file name pattern to read
	*	@return Map FilePath -> FileContentAsStream
	*/
	@Processor  
	public Map<String, InputStream> readFiles(@Payload String payload, String fileNamePattrn) {
		_logger.debug("Start->readFiles");
		Map<String, InputStream> files = new HashMap<>();
		
        NtlmPasswordAuthentication auth = this.getNtlmAuth();
		
		String path = getSambaConnectionString();
		_logger.debug("Target location => " + path);
			try {
				SmbFile resource = new SmbFile(path, auth);
				if (resource.isFile()) {
					files.put(resource.getPath(), readFileContents(resource));
					_logger.debug("Resource Found=> " + resource.getPath());
				} else if (resource.list().length > 0) {
					SmbFile[] fileList = resource.listFiles(fileNamePattrn);
					_logger.debug("Resource is a Folder. Below are the files in this folder => "+resource.getPath());
					for(SmbFile file : fileList) {
						files.put(file.getPath(), readFileContents(file));
						_logger.debug(file.getPath());
					}
				}
			} catch (Exception e) {
				_logger.error("Something went wrong while accessing the resource",e);
				files = null;
			}

		_logger.debug("End->readFiles");
		return files;
	}
	
	/**
	* 	This processor will return a list of fully qualified file name present at the -
	* 	configured folder based on a file name pattern
	*
	*	@param payload 
	*	@param fileNamePattrn The file name pattern to read
	*	@return List of files
	*/
	@Processor  
	public List<String> readFileNames(@Payload String payload, String fileNamePattrn) {
		_logger.debug("Start->readFileNames");
		List<String> files = new ArrayList<>();
		
        NtlmPasswordAuthentication auth = this.getNtlmAuth();
		
		String path = getSambaConnectionString();
		_logger.debug("Target location => " + path);
			try {
				SmbFile resource = new SmbFile(path, auth);
				if (resource.isFile()) {
					files.add(resource.getPath());
					_logger.debug("Resource Found=> " + resource.getPath());
				} else if (resource.list().length > 0) {
					SmbFile[] fileList = resource.listFiles(fileNamePattrn);
					_logger.debug("Resource is a Folder. Below are the files in this folder => "+resource.getPath());
					for(SmbFile file : fileList) {
						files.add(file.getPath());
						_logger.debug(file.getPath());
					}
				}
			} catch (Exception e) {
				_logger.error("Something went wrong while accessing the resource",e);
				files = null;
			}

		_logger.debug("End->readFileNames");
		return files;
	}
	
	/**
	*	Message processor that can be directly called to read a specified File	
	*
	*	@param fileName The name of the file to read (excluding path)
	*	@return returns file contents as byte array
	*/
	@Processor
	public InputStream readFile(String fileName) {			
		_logger.debug("Start->readFile");

        NtlmPasswordAuthentication auth = this.getNtlmAuth();
		
		String path = getSambaConnectionString() + fileName;
		try {
			SmbFile resource = new SmbFile(path,auth);		
			return readFileContents(resource);
			
		} catch (Exception e) {
			_logger.error("Something went wrong while reading the file",e);	
		}	
		
		_logger.debug("End->readFile");  
		return null;
	} 
	

	
	/**
    * 	This processor will delete any file specified by the file name
    *
	*	@param fileName The name of the file you want to delete
	*	@return returns boolean to indicate successful deletion of a file
    */
    @Processor
    public boolean deleteFile(String fileName) {
         
    	_logger.debug("Start->deleteFile");
    	
    	NtlmPasswordAuthentication auth = this.getNtlmAuth();
		
		String path = getSambaConnectionString() + fileName;
		
         boolean status = true;
         try {
        	 SmbFile resource = new SmbFile(path, auth);	
        	 resource.delete();           
         } catch (Exception e) {
             _logger.error("Something went wrong while deleting the resource", e);
             status = false;
         }
         
         _logger.debug("End->deleteFile");  
         return status;
     }
    
	protected InputStream readFileContents(SmbFile sFile) {
		
		SmbFileInputStream inFile = null;
		
		try {
			inFile = new SmbFileInputStream(sFile);
			byte[] sBytes = IOUtils.toByteArray(inFile);
			inFile.close();
			return new ByteArrayInputStream(sBytes);
			
		} catch (Exception e) {
			
			try {
				if(inFile != null)
					inFile.close();
			} catch (IOException ignored) {}
			_logger.error("Could not read the file=> "+sFile.getPath(), e);	
		}
		
		return null;
			
	}  
	
	protected NtlmPasswordAuthentication getNtlmAuth() {
    	String domain = "";
    	if(config.getDomain() != null) {
    		domain  = config.getDomain();
    	}
        return new NtlmPasswordAuthentication(domain, config.getUsername(), config.getPassword());
    }
    
    protected String getSambaConnectionString() {
    	String FRONT_SLASH = "/";
    	String BACK_SLASH = "\\";
    	String folder = config.getFolder().trim();
    	String connStringPostFix = FRONT_SLASH;
    	if(folder.endsWith(FRONT_SLASH) || folder.endsWith(BACK_SLASH)) connStringPostFix = "";
    			
    	StringBuilder connStr = new StringBuilder();
    	connStr.append("smb://")
    		   .append(config.getHost()).append(FRONT_SLASH)
    		   .append(config.getFolder()).append(connStringPostFix);
    	
    	return connStr.toString();
    }

    public ConnectorConfig getConfig() {
        return config;
    }

    public void setConfig(ConnectorConfig config) {
        this.config = config;
    }

}