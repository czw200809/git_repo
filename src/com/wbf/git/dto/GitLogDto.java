package com.wbf.git.dto;

import java.util.Date;

import org.eclipse.jgit.revwalk.RevCommit;

public class GitLogDto {
    
   public String revision;
   public Date commitDate;
   public Date modifyDate;
   public String author;
   public String committer;
   public String commitMessage;

   public GitLogDto()
   {
	   
   }
   
   public GitLogDto(RevCommit commit)
   {
	   if (commit != null)
	   {
		   revision = commit.getName();
		   author = commit.getAuthorIdent().getName();
		   modifyDate = commit.getAuthorIdent().getWhen();
		   committer = commit.getCommitterIdent().getName();
		   commitMessage = commit.getFullMessage();
		   commitDate = commit.getAuthorIdent().getWhen();
	   }
   }

	public String getRevision() {
		return revision;
	}
	
	public void setRevision(String revision) {
		this.revision = revision;
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
	
	public Date getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(Date commitDate) {
		this.commitDate = commitDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
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
   
}
