package com.wbf.git.dto;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.FileMode;

public class GitDiffStatusDto 
{    
    public static final int DIFF_STATUS_ADD = 1;
    public static final int DIFF_STATUS_COPY = 2;
    public static final int DIFF_STATUS_DELETE = 3;
    public static final int DIFF_STATUS_MODIFY = 4;
    public static final int DIFF_STATUS_RENAME = 5;
    
    public final static int GIT_KIND_FILE = 6;
    public final static int GIT_KIND_DIR = 7;
	public final static int GIT_KIND_HIDDEN = 8;
	public final static int GIT_KIND_UNKNOWN = 9;
    
    public int modificationType; //the diff status enum
    public int kind;	//file kind
    public boolean propertiesModified; //if the property modified
    public String path; //the relative path (ex. dir/fiel.txt,not the name)
    public String url; //the url
    
    public GitDiffStatusDto()
    {
    	
    }
    
    public GitDiffStatusDto(DiffEntry entry, String gitRoot)
    {
    	if (entry != null)
    	{
    		ChangeType changeType = entry.getChangeType();
    		FileMode fileMode = entry.getNewMode();
    		switch(changeType){
    			case ADD : 
    				modificationType = 1;
    					break;
    			case COPY : 
    				modificationType = 2;
						break;
    			case DELETE : 
    				modificationType = 3;
						break;
    			case MODIFY : 
    				modificationType = 4;
						break;
    			case RENAME : 
    				modificationType = 5;
						break;
				default : 
						break;
    		}
    		
    		
    		path = entry.getNewPath();
    		url = gitRoot + "/" + path;
    	}
    }
    
	public int getModificationType() {
		return modificationType;
	}
	public void setModificationType(int modificationType) {
		this.modificationType = modificationType;
	}
	public int getKind() {
		return kind;
	}
	public void setKind(int kind) {
		this.kind = kind;
	}
	public boolean isPropertiesModified() {
		return propertiesModified;
	}
	public void setPropertiesModified(boolean propertiesModified) {
		this.propertiesModified = propertiesModified;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
    
}
