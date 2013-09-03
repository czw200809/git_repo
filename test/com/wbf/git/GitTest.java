package com.wbf.git;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase
{	
	public void testGetLog() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		//String untilRev = "32906bcc2b65301647902d1f0c20f0be0777e4da";//fourth
		String untilRev = "a028a72966e72fa45976ebccae1a166af3c77094";//fourth
		String startRev = "afb3add42ddead3e40847ecad06d533a96076c58";//first
		String branchName = "b1";
		
		GitService.getLog(gitRoot, branchName, startRev, untilRev);
	}
	
	public void testOpenRemoteRepo() throws Exception
	{
		String gitRoot = "git@github.com:czw200809/git_repo.git";
		GitService.openRemoteRepo(gitRoot);
	}
}
