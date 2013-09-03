package com.wbf.git.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;

import com.wbf.git.dto.GitLogDto;

public class GitService 
{	
	private final static String GIT = ".git";
	private final static String HEAD = "HEAD";
    private final static String REF_REMOTES = "refs/remotes/origin/";
    
    //某个时间段内的log信息
    public static List<GitLogDto> getLog(String gitRoot, String branchName, String startRev, String untilRev) throws Exception
    {
    	Git git = getGit(gitRoot);
    	Repository repository = git.getRepository();
    	ObjectId startObjId = repository.resolve(startRev);
    	ObjectId untilObjId = repository.resolve(untilRev);
    	
    	 Ref ref = repository.getRef(branchName);
         ObjectId branchObjId = ref.getObjectId();
         
         Iterable<RevCommit> revCommits = git.log().add(branchObjId).addRange(startObjId, null).call();
         
         GitLogDto logDto = null;
         List<GitLogDto> logDtoList = new ArrayList<GitLogDto>();
         for (RevCommit rev : revCommits)
         {
        	logDto = new GitLogDto(rev);
     		logDtoList.add(logDto);
         }
         
         return logDtoList;
    }
    
    //open repo
    public static Git getGit(String gitRoot) throws Exception
    {
    	File rootDir = new File(gitRoot);
    	if (new File(gitRoot + File.separator + GIT).exists() == false) {  
    		Git.init().setDirectory(rootDir).call();  
        }
    	 
    	Git git = Git.open(rootDir);
    	return git;
    }
    
    public static void openRemoteRepo(String gitRoot) throws Exception 
    {
    	
    }
    
}
