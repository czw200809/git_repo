package com.wbf.git;

import com.wbf.git.service.GitService;

import junit.framework.TestCase;

public class GitTest extends TestCase {
	
	//测试getLog()
	public void testGetLog() throws Exception {
		
		String gitRoot = "https://github.com/czw200809/test_git_project.git";
		//String gitRoot = "D:/MyEclipse_Space/test_git_project/.git";
		String revision = "dc7b9a2fbab9f503bf2fcfa51db1e06a78ce424c";
		//GitService.getLog(gitRoot, "HEAD");
		GitService.getLog(gitRoot, revision);
		//GitService.getLog(gitRoot, null);
	}
	
	//测试比较差异，全部文件或某个指定文件
	public void testDiffRevision() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project";
		String filePath = "src/com/wbf/git/dto/GitLogDto.java";
		String childRevision = "dc7b9a2fbab9f503bf2fcfa51db1e06a78ce424c";//最新提交
		String parentRevision = "7b19fd70854b1abc27d359c297dd26e026733776";//第一个提交
		GitService.getDiffBetweenRevisions(gitRoot, parentRevision, childRevision, filePath);
	}
	
	//获取某个文件指定版本的内容
	public void testGetContent() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project/.git";
		String filePath = "src/com/wbf/git";
		//String revision = "2221ee10766ba0539d33b913f8fb0cf2ef390019";
		String revision = "65c3f43e594bb6f502d51609e58e30973d5b35b1";
		byte[] bytes = GitService.getContentWithSpecifiedRevision(null, gitRoot, revision, filePath);
		
		if (bytes != null)
		{
			String str = new String(bytes, "gb2312");
			System.out.println(str);
		}
	}
	
	public void testTraverseEntry() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project";
		//String filePath = "src/com/wbf/git/TestDemo.java";
		String filePath = "src/com/wbf/git";
		//String revision = "2221ee10766ba0539d33b913f8fb0cf2ef390019";
		String revision = "65c3f43e594bb6f502d51609e58e30973d5b35b1";
		
		GitService.traverseDirEntry(gitRoot, revision, filePath);
	}
	
	public void testChangeType() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project";
		//String filePath = "src/com/wbf/git/TestDemo.java";
		String filePath = "src/com/wbf/git/service/GitService.java";
		//String revision = "2221ee10766ba0539d33b913f8fb0cf2ef390019";
		String revision = "65c3f43e594bb6f502d51609e58e30973d5b35b1";
		GitService.getChangeType(gitRoot, revision, filePath);
	}
	
	public void testListDirEntry() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project";
		//String filePath = "src/com/wbf/git/TestDemo.java";
		String filePath = "src/com/wbf/git";
		//String revision = "2221ee10766ba0539d33b913f8fb0cf2ef390019";
		String revision = "65c3f43e594bb6f502d51609e58e30973d5b35b1";
		GitService.listDirEntryBySpecificPath(gitRoot, revision, filePath);
	}
	
	public void testRevCommit() throws Exception {
		
		String gitRoot = "D:/MyEclipse_Space/test_git_project/.git";
		String revision = "dc7b9a2fbab9f503bf2fcfa51db1e06a78ce424c";
		String filePath = "src/com/wbf/git/dto/GitLogDto.java";
		GitService.getRevCommit(gitRoot, filePath, revision);
	}
	
	public void testBuildRepository() throws Exception {
		
		GitService.buildRepository();
	}
}
