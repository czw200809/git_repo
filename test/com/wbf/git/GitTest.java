package com.wbf.git;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase
{	
	public void testGetLog() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String branchName = "master";
		String filePath = "src/Demo.java";
		
		GitService.getLog(gitRoot, branchName, filePath);
		
	}
	
	public void testGetLog1() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String branchName = "master";
		Date d = new Date();
		System.out.println();
		
		Date startDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2000-01-01 00:00:00");
		System.out.println(startDate.getTime());
		Date untilDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2013-08-31 00:00:00");
		System.out.println(untilDate.getTime());
		GitService.getLog(gitRoot, branchName, startDate, untilDate, null);

	}
	
	public void testGetLog2() throws Exception
	{
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String branchName = "master";
		
		String startRev = "e94806ad2dc17b475acf76ab72b17fe44b2db80e";//second commit
		String untilRev = "a028a72966e72fa45976ebccae1a166af3c77094";//30 last one
		
		GitService.getLog(gitRoot, branchName, startRev, untilRev, null);
	}
	
	public void testPrint() throws Exception
	{
		GitService.print();
	}
}
