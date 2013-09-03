package com.wbf.git;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase
{	
	public void testGetLog() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String untilRev = "a775906e5eef4d9f974a7ccce6fdb98568d19769";// b1 new
		//String untilRev = "8390295535b0eeb002e5f84ede9b2d960bc5d66b";//fourth
		String startRev = "afb3add42ddead3e40847ecad06d533a96076c58";//first
		String branchName = "master";
		
		//GitService.getLog(gitRoot, startRev, untilRev, null);
		GitService.getLog(gitRoot, branchName);
	}
}
