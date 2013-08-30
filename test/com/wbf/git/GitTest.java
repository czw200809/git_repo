package com.wbf.git;

import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase
{
	public void testListDirEntry() throws Exception
	{
		String revision = "d7002489eb4c2a2073cee1ea2119f697ef607c80";
		String gitRoot = "D:/MyEclipse_Space/git_project";
		String filePath = "src/com/wbf/git/dto";
		
		GitService.listDirEntry(gitRoot, filePath, revision);
	}
	
	public void testGetLog() throws Exception
	{
		String startRevision = "4937b6fd88b31bad2c4efa8e38daa915f76d6c22";
		String untilRevision = "ede6dcdeddc9de83f6ac9912f93d6f4f4ad5062e";
		String gitRoot = "D:/MyEclipse_Space/git_project/.git";
		String filePath = "src/com/wbf/git/dto/GitDiffStatusDto.java";
		
		//GitService.getLog(gitRoot, startRevision, untilRevision, filePath);
		//GitService.getLog(gitRoot, startRevision, untilRevision, null);
		//GitService.getSpecificLog(gitRoot, untilRevision, filePath);
		GitService.getLogDtoList(gitRoot, filePath);
	}
	
	public void testRepoBrowser() throws Exception
	{
		//String gitRoot = "D:/MyEclipse_Space/git_project/.git";
		String gitRoot = "https://github.com/czw200809/repo_git.git";
		GitService.repoBrowser(gitRoot);
	}
	
	public void testCloneRemoteRepository() throws Exception
	{
		GitService.cloneRemoteRepository();
	}
}
