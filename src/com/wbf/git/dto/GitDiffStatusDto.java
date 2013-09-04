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
	public final static int GIT_KIND_UNKNOWN = 9;
    
    public int modificationType;
    public int kind;
    public boolean propertiesModified;
    public String path;
    public String url;
    
    public GitDiffStatusDto()
    {
    	
    }
    
    public GitDiffStatusDto(DiffEntry entry, String gitRoot)
    {
    	if (entry != null)
    	{
    		if (entry.getChangeType().name().equals("ADD"))
    			modificationType = 1;
    		else if (entry.getChangeType().name().equals("COPY"))
    			modificationType = 2;
    		else if (entry.getChangeType().name().equals("DELETE"))
    			modificationType = 3;
    		else if (entry.getChangeType().name().equals("MODIFY"))
    			modificationType = 4;
    		else//RENAME
    			modificationType = 5;
    		
    		int bits = entry.getNewMode().getBits();
    		FileMode mode = entry.getNewMode();
    		
    		switch (bits & mode.TYPE_MASK)
			{
			    case 0:
			        if (bits == 0)
			        {
			            kind = GIT_KIND_NONE;
			        }
			        break;
			
			    case 16384:
			    	
			    	kind = GIT_KIND_DIR;
			    	break;
			
			    case 32768:
			        kind = GIT_KIND_FILE;
			        break;
			    default:
			    	kind = GIT_KIND_UNKNOWN;
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
