package com.wbf.git;

import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

import com.wbf.git.service.GitService;
import com.wbf.git.service.GitService_1;

import junit.framework.TestCase;

public class GitTest extends TestCase
{	
	public void testGetLog() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String untilRev = "32906bcc2b65301647902d1f0c20f0be0777e4da";
		String startRev = "afb3add42ddead3e40847ecad06d533a96076c58";
		String branchName = "master";
		
		GitService.getLog(gitRoot, branchName, startRev, untilRev);
	}
}
