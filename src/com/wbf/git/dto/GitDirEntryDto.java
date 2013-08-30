package com.wbf.git.dto;

import java.util.Date;

public class GitDirEntryDto {

	public String name;             //filename
    public int kind;         //file or dir
    public long size;               //for dir, size is 0
    //public boolean hasProperties;   //not include svn: properties
    public String commitRevision;     //last modified revision
    public Date commitDate;        //last modified date/time
    public String commitAuthor;       //last modify's author
    public String commitMessage;    //last modify's commit message.
    public String relativePath;
	//public SvnLockDto lock;         //the lock info
    public String url;              //the url 
    public String repositoryRoot;    //the root url, not server, it's repository root url.
    
    public String getRelativePath() {
		return relativePath;
	}
	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getKind() {
		return kind;
	}
	public void setKind(int kind) {
		this.kind = kind;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getCommitRevision() {
		return commitRevision;
	}
	public void setCommitRevision(String commitRevision) {
		this.commitRevision = commitRevision;
	}
	public Date getCommitDate() {
		return commitDate;
	}
	public void setCommitDate(Date commitDate) {
		this.commitDate = commitDate;
	}
	public String getCommitAuthor() {
		return commitAuthor;
	}
	public void setCommitAuthor(String commitAuthor) {
		this.commitAuthor = commitAuthor;
	}
	public String getCommitMessage() {
		return commitMessage;
	}
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getRepositoryRoot() {
		return repositoryRoot;
	}
	public void setRepositoryRoot(String repositoryRoot) {
		this.repositoryRoot = repositoryRoot;
	}
    
    
}
