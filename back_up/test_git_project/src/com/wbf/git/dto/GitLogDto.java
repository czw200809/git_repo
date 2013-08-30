package com.wbf.git.dto;

import java.util.Date;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public class GitLogDto {

	public String revision; //a revison that have modification to the object
    public Date date; //when	
    public String author;   //the modify author
    public String committer; //who commits
    public String commitMessage; //The cause of the modification
    
    public List<GitLogDto> parents;
    
    public GitLogDto()
    {
 	   
    }
    
    public GitLogDto(RevCommit commit)
    {
 	   if (commit != null)
 	   {
 		   revision = commit.getName();
 		   author = commit.getAuthorIdent().getName();
 		   committer = commit.getCommitterIdent().getName();
 		   commitMessage = commit.getFullMessage();
 		   date = commit.getAuthorIdent().getWhen();
 	   }
    }

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getCommitter() {
		return committer;
	}

	public void setCommitter(String committer) {
		this.committer = committer;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public List<GitLogDto> getParents() {
		return parents;
	}

	public void setParents(List<GitLogDto> parents) {
		this.parents = parents;
	}
    
    
}

