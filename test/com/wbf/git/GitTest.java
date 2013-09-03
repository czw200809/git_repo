package com.wbf.git;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase
{	
	public void testGetLog() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		//String untilRev = "32906bcc2b65301647902d1f0c20f0be0777e4da";//fourth
		String untilRev = "8390295535b0eeb002e5f84ede9b2d960bc5d66b";//fourth
		String startRev = "afb3add42ddead3e40847ecad06d533a96076c58";//first
		String branchName = "b1";
		
		GitService.getLog(gitRoot, startRev, untilRev, null);
	}
}
