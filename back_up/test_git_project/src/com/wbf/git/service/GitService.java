package com.wbf.git.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.IndexDiff;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.FS;

import com.wbf.git.dto.GitDiffStatusDto;
import com.wbf.git.dto.GitLogDto;

public class GitService {
	
	public static final String GIT = Constants.DOT_GIT;
	public static final String HEAD = Constants.HEAD;
	public static final String MASTER = Constants.MASTER;
	public static final String ORIGIN = Constants.DEFAULT_REMOTE_NAME;
	
	public static void getLog(String gitRoot, String revision) throws Exception 
	{
		File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();

		Iterable<RevCommit> logs = null;
		Iterator<RevCommit> iter = null;
		List<GitLogDto> rstList = null;
		GitLogDto logDto = null;
		if (git != null)
		{
			rstList = new ArrayList<GitLogDto>();
			if (revision != null)
			{
				ObjectId objId = repository.resolve(revision);
				logs = git.log().add(objId).call();
				int count = 1;
				GitLogDto gitLogDto = null;
				for (RevCommit revCommit : logs)
				{
					if (count == 1) 
					{
						gitLogDto = new GitLogDto(revCommit);
						rstList.add(gitLogDto);
						count++;
					} 
					else 
					{
						logDto = new GitLogDto(revCommit);
						gitLogDto.parents = new ArrayList<GitLogDto>();
						gitLogDto.parents.add(logDto);
					}
				}
			} 
			else
			{
				logs = git.log().call();
				for (iter = logs.iterator(); iter.hasNext();) 
				{
					RevCommit commit = iter.next();
					logDto = new GitLogDto(commit);
					rstList.add(logDto);
				}
			}
		}

		if (rstList != null && rstList.size() > 0) 
		{
			for (int i = 0; i < rstList.size(); i++) 
			{
				GitLogDto dto = rstList.get(i);
				System.out.println(dto.getRevision() + " ," + dto.getAuthor() + " ," + dto.getCommitter() + ", " + dto.getCommitMessage());
			}
		}
	}

	//比较两个版本之间的差异(某个文件或全部)
	public static void getDiffBetweenRevisions(String gitRoot, String Child, String Parent, String filePath) throws Exception
	{
		File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();
		
        ObjectReader reader = repository.newObjectReader();  
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  
      
        try {  
            ObjectId old = repository.resolve(Child + "^{tree}");  
            ObjectId head = repository.resolve(Parent+"^{tree}");  
            oldTreeIter.reset(reader, old);  
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();  
            newTreeIter.reset(reader, head);  
            
            List<DiffEntry> diffs = null;
            //filePath != null说明是要获取指定文件在这两个版本之间的差异(变更比较)
            if (filePath != null)
            {
            	diffs = git.diff().setPathFilter(PathFilter.create("src/com/wbf/git/dto/GitLogDto.java"))
            				.setNewTree(newTreeIter)  
            				.setOldTree(oldTreeIter)  
            				.call();
            }
            else	//获取这两个版本之间的全部差异(变更比较)
            {
            	diffs = git.diff().setPathFilter(null)
							.setNewTree(newTreeIter)  
							.setOldTree(oldTreeIter)  
							.call();
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();  
            DiffFormatter df = new DiffFormatter(out);  
            df.setRepository(git.getRepository());  
              
            for (DiffEntry diffEntry : diffs) {  
                 df.format(diffEntry);  
                 String diffText = out.toString("gb2312");  
                 System.out.println(diffText);  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
        } 
    }  
	
	//获取某个文件在某个版本的RevCommit
	public static List<RevCommit> getRevCommit(String gitRoot, String filePath, String revision) throws Exception
	{
		File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();
		
		List<RevCommit> rstList = null;
		Iterable<RevCommit> logs = null;
		
		if (git != null)
		{
			if (revision != null)
			{	
				rstList = new ArrayList<RevCommit>();
				ObjectId objId = repository.resolve(revision);
				logs = git.log().addPath(filePath).add(objId).call();
				
				for (RevCommit revCommit : logs)
				{
					rstList.add(revCommit);
				}
			} 
		}
		
		return rstList;
	}
	
	//某个文件指定版本的内容
    public static byte[] getContentWithSpecifiedRevision(Repository repository, String gitRoot, String revision, String filePath) throws Exception
    {
		if (repository == null)
		{
			File file = new File(gitRoot);
			Git git = Git.open(file);
			repository = git.getRepository();
		}
		
		byte[] bytes = null;
		if (repository != null)
		{
			RevWalk walk = new RevWalk(repository);
		    ObjectId objId = repository.resolve(revision);
		    
		    RevCommit revCommit = null;
		    RevTree revTree = null;
		    if (walk != null)
		    {		    	
		    	revCommit = walk.parseCommit(objId);
		    	if (revCommit != null)
			    {		    	
			    	revTree = revCommit.getTree();  
			    }
		    }
		    
		    TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, revTree);//relative file path
		    if (treeWalk == null)  
		        return null;  
		    ObjectId blobId = treeWalk.getObjectId(0);
		    ObjectLoader loader = repository.open(blobId);
		    if (loader != null)
		    	bytes = loader.getBytes(); 
		}
    	
		return bytes; 
    }
    
    //获取当前文件上一个版本的内容
    public static byte[] getPreRevisionContent(String gitRoot, String revision, String filePath) throws Exception
    {
    	File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();
		
		byte[] bytes = null;
		if (repository != null)
		{
			RevWalk walk = new RevWalk(repository);    
			ObjectId objId = repository.resolve(revision);//通过版本号分解，得到版本对象(String>>>object)
			RevCommit revCommit = null;
			String preVision = null;
			if (objId != null)
			{
				revCommit = walk.parseCommit(objId);
				if (revCommit != null)
				{
					preVision = revCommit.getParent(0).getName();//取得上一版本号 
				}
			}
			
			bytes = getContentWithSpecifiedRevision(repository, gitRoot, preVision, filePath);
		}
    	
        return bytes;
    }
    
    public static List<Map<String, Object>> traverseDirEntry(String gitRoot, String revision, String filePath) throws Exception
    {
    	File rootFile = new File(gitRoot + "/" + GIT);
		Git git = Git.open(rootFile);
		Repository repository = git.getRepository();
		String fileRootPath = gitRoot + "/" + filePath;
		
		boolean isTrue = false;
		List<Map<String, Object>> rstList = new ArrayList<Map<String, Object>>();
		if (repository != null)
		{
			FileTreeIterator fileIter = new FileTreeIterator(repository);
			while(fileIter != null)
			{
				if (!fileIter.eof() && rstList.size() <= 0)
				{
					rstList = getFileInfo(fileIter.getEntryFile(), fileRootPath, filePath, isTrue, rstList);
					fileIter.next(1);
				}
				else
				{
					fileIter = null;
				}
			}
		}
		
		if (rstList != null && rstList.size() > 0)
		{
			for (int i = 0; i < rstList.size(); i++)
			{
				Map<String, Object> map = rstList.get(i);
				System.out.println(map.get("name") + ", " + map.get("path") + ", " + map.get("relativePath") + ", " + map.get("fileType") + ", " + map.get("fileSize"));
			}
		}
		
		return rstList;
    } 
    
    public static List<Map<String, Object>> getFileInfo(File file, String filePath, String relativePath, boolean isTrue, List<Map<String, Object>> rstList) throws Exception
    {
    	File[] files = null;
    	if (file != null)
    	{	
    		if (file.isDirectory())
    		{
    			if (isTrue)
	    		{	
    				Map<String, Object> map = new HashMap<String, Object>();
    				map.put("name", file.getName());
    				map.put("path", file.getPath());
    				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
    				map.put("fileType", GitDiffStatusDto.GIT_KIND_DIR);
    				map.put("fileSize", 0);
	    			rstList.add(map);
	    			return rstList;
	    		}
	    		
	    		files = file.listFiles();
	    		String path = file.getPath().replace("\\", "/");
	    		if (filePath.equals(path))
	    		{
	    			isTrue = true;    			
	    		}
	    		
	    		for (File subFile : files)
	    		{
	    			getFileInfo(subFile, filePath, relativePath, isTrue, rstList);
	    		}
    		}
    		else if (file.isFile() && isTrue)
    		{	
    			Map<String, Object> map = new HashMap<String, Object>();
				map.put("name", file.getName());
				map.put("path", file.getPath());
				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
				map.put("fileType", GitDiffStatusDto.GIT_KIND_FILE);
				map.put("fileSize", file.length());
				
    			rstList.add(map);
    		}
    		else 
    		{
    			if (isTrue)
    			{
    				Map<String, Object> map = new HashMap<String, Object>();
    				map.put("name", file.getName());
    				map.put("path", file.getPath());
    				map.put("relativePath", (relativePath + "/" + file.getName()).replace("/", "\\"));
    				map.put("fileType", GitDiffStatusDto.GIT_KIND_NONE);
    				map.put("fileSize", 0);
        			rstList.add(map);
    			}
    		}
    	}
    	
    	return rstList;
    }
    
  //遍历文件目录，从指定的path开始遍历，返回一级子节点
    public static void listDirEntryBySpecificPath(String gitRoot, String revision, String filePath) throws Exception
    {   
    	File rootFile = new File(gitRoot + "/" + GIT);
		Git git = Git.open(rootFile);
		Repository repository = git.getRepository();
		String fileRootPath = gitRoot + "/" + filePath;
		FileTreeIterator fileIter = new FileTreeIterator(new File(fileRootPath), FS.DETECTED, null);

		File file = null;
		String fileName = null;
		List<String> rstList = new ArrayList<String>();
		while(fileIter != null)
		{
			if (!fileIter.eof())
			{
				file = fileIter.getEntryFile();
				fileName = file.getName();
				rstList.add(fileName);
				fileIter.next(1);
			}
			else
			{
				fileIter = null;
			}
		}
		
		if (rstList != null && rstList.size() > 0)
		{
			for (String name : rstList)
			{
				System.out.println(name);
			}
		}
    }
    
    //获取单个文件的在某个版本的changeType
    public static int getChangeType(String gitRoot, String revision, String filePath) throws Exception
    {
    	File file = new File(gitRoot);
		Git git = Git.open(file);
		Repository repository = git.getRepository();
		
		ObjectReader reader = repository.newObjectReader();  
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  

		ObjectId old = repository.resolve(HEAD + "^{tree}");  
		ObjectId head = repository.resolve(revision+"^{tree}");  
		oldTreeIter.reset(reader, old);  
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();  
		newTreeIter.reset(reader, head);  
		
		List<DiffEntry> diffs = null;
		//filePath != null说明是要获取指定文件在这两个版本之间的差异(变更比较)
		if (filePath != null)
		{
			diffs = git.diff().setPathFilter(PathFilter.create(filePath))
						.setNewTree(newTreeIter)  
						.setOldTree(oldTreeIter)  
						.call();
		}
		
		for (DiffEntry diff : diffs)
		{
			System.out.println(diff.getChangeType());
		}
		
		return 0;
    } 
    
    public static void buildRepository() throws Exception {
    	
    	FileRepositoryBuilder builder = new FileRepositoryBuilder();
    	Repository repo = builder.setGitDir(new File("https://github.com/czw200809/test_git_project.git"))
    	  .readEnvironment() // scan environment GIT_* variables
    	  .findGitDir() // scan up the file system tree
    	  .build();
    	
    	Git git = new Git(repo);
    	LogCommand logCommand = git.log();
    	ObjectId endId = repo.resolve("HEAD");
    	ObjectId startId = repo.resolve("HEAD");
    	logCommand.addRange(startId, endId);
    	Iterable<RevCommit> logs = logCommand.call();
    	
    	int a = 1;
    }
    
    
    
}
