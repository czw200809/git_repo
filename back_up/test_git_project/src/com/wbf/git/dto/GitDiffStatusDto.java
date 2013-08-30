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
	public final static int GIT_KIND_NONE = 8;
	public final static int GIT_KIND_MASK = 9;
	public final static int GIT_KIND_GITLINK = 10;
	public final static int GIT_KIND_SYSLINK = 11;
	public final static int GIT_KIND_UNKNOWN = 12;
    
    public int actionType; //the diff status enum
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
    		int objectType = fileMode.getObjectType();
    		switch(changeType){
    			case ADD : 
    					actionType = 1;
    					break;
    			case COPY : 
						actionType = 2;
						break;
    			case DELETE : 
						actionType = 3;
						break;
    			case MODIFY : 
						actionType = 4;
						break;
    			case RENAME : 
						actionType = 5;
						break;
				default : 
						break;
    		}
    		
    		switch(objectType) {
				case FileMode.TYPE_FILE : 
										kind = 6;
										break;
				case FileMode.TYPE_TREE : 
										kind = 7;
										break;
				case FileMode.TYPE_MISSING : 
										kind = 8;
										break;
				case FileMode.TYPE_MASK : 
										kind = 9;
										break;
				case FileMode.TYPE_GITLINK : 
										kind = 10;
										break;
				case FileMode.TYPE_SYMLINK : 
										kind = 11;
										break;
				default :
						kind = 12;
						break;
    		}
    		
    		path = entry.getNewPath();
    		url = gitRoot + "/" + path;
    	}
    }
    
	public int getActionType() {
		return actionType;
	}
	public void setActionType(int actionType) {
		this.actionType = actionType;
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
