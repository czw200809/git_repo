package com.wbf.git.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import com.wbf.git.dto.GitDiffStatusDto;
import com.wbf.git.dto.GitDirEntryDto;
import com.wbf.git.dto.GitLogDto;

public class GitService 
{	
	private final static String GIT = ".git";
	private final static String HEAD = "HEAD";
    private final static String REF_REMOTES = "refs/remotes/origin/";
    
    public static Map<String, Object> getGit(String gitRoot, String branchName) throws Exception
    {	
    	Map<String, Object> rstMap = new HashMap<String, Object>();
    	String dirStr = System.getProperty("java.io.tmpdir") + "tmp" + System.currentTimeMillis();
    	rstMap.put("dirStr", dirStr);
    	File tmpDir = new File(System.getProperty("java.io.tmpdir"), "tmp"
				+ System.currentTimeMillis());
		tmpDir.mkdirs();
		
		Git git = Git.cloneRepository()
				.setBare(false)
				.setBranch(branchName)
				.setDirectory(tmpDir)
				.setURI(gitRoot)
				.setProgressMonitor(new TextProgressMonitor())
				.call();
	
				rstMap.put("git", git);
		
		return rstMap;
    }
    
    public static void del(File file)
	{
		if (file.isDirectory())
		{
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				del(files[i]);
			}
		}
		file.delete();
	}
    
    //��ȡָ����֧(��ָ��Ŀ¼)������log
    public static List<GitLogDto> getLog(Map<String, Object> rstMap, String gitRoot, String branchName, String filePath) throws Exception
    {
    	boolean isNew = false;
    	if (rstMap == null)
    	{
    		isNew = true;
    		rstMap = getGit(gitRoot, branchName);
    	}
        Git git = (Git)rstMap.get("git");
        //Repository repo = git.getRepository();
        
        List<GitLogDto> logDtoList = null;
        //ObjectId objId = repo.resolve(branchName);
        
        Iterable<RevCommit> revCommits = null;
        if (filePath == null)
        	//revCommits = git.log().add(objId).call();
        	revCommits = git.log().call();
        else
        	//revCommits = git.log().addPath(filePath).add(objId).call();
        	revCommits = git.log().addPath(filePath).call();
        if (revCommits != null)
        {
        	logDtoList = new ArrayList<GitLogDto>();
        	RevCommit revCommit = null;
        	GitLogDto logDto = null;
        	for (Iterator<RevCommit> iter = revCommits.iterator(); iter.hasNext();)
        	{
        		revCommit = iter.next();
        		logDto = new GitLogDto(revCommit);
        		logDtoList.add(logDto);
        	}
        }
        
        if (isNew)
        	del(new File((String)rstMap.get("dirStr")));
        
    	return logDtoList;
    }
    
    //��ȡָ����֧��ĳ��ʱ����ڵ�log
    public static List<GitLogDto> getLog(Map<String, Object> rstMap, String gitRoot, String branchName, Date startDate, Date untilDate, String filePath) throws Exception
    {
        List<GitLogDto> logDtoList = getLog(rstMap, gitRoot, branchName, filePath);
        
        for (Iterator<GitLogDto> iter = logDtoList.iterator();iter.hasNext();)
        {
        	GitLogDto logDto = iter.next();
        	Date d = logDto.commitDate;
        	long time = d.getTime();
        	long startTime = startDate.getTime();
        	long untilTime = untilDate.getTime();
        	if (time < startTime && time > untilTime)
        	{
        		iter.remove();
        	}
        }
        
    	return logDtoList;
    }
    
    //��ȡָ����֧��startRev-untilRev֮��İ汾
    public static List<GitLogDto> getLog(Map<String, Object> rstMap, String gitRoot, String branchName, String startRev, String untilRev, String filePath) throws Exception
    {
    	boolean isNew = false;
    	if (rstMap == null)
    	{    		
    		rstMap = getGit(gitRoot, branchName);
    		isNew = true;
    	}
        Git git = (Git)rstMap.get("git");
        Repository repo = git.getRepository();
        
        List<GitLogDto> logDtoList = null;

        RevWalk revWalk = new RevWalk(repo);
        ObjectId startObjId = repo.resolve(startRev);
        ObjectId untilObjId = repo.resolve(untilRev);
        RevCommit rev1 = revWalk.parseCommit(startObjId);
        RevCommit rev2 = revWalk.parseCommit(untilObjId);
        Date startDate = rev1.getCommitterIdent().getWhen();
        Date untilDate = rev2.getCommitterIdent().getWhen();
        
        logDtoList = getLog(rstMap, gitRoot, branchName, startDate, untilDate, filePath);
        
        if (isNew)
        	del(new File((String)rstMap.get("dirStr")));
        
    	return logDtoList;
    }
	
    public static String getDiff(String gitRoot, String branchName, String rev1, String rev2, String filePath) throws Exception
	{
		Map<String, Object> rstMap = getGit(gitRoot, branchName);
        Git git = (Git)rstMap.get("git");
        Repository repository = git.getRepository();
        
        RevWalk rw = new RevWalk(repository);
        ObjectId objId1 = null;
        ObjectId objId2 = null;
        RevCommit rc1 = null;
        RevCommit rc2 = null;
        
        if (rev1 != null && rev2 != null)
        {
        	objId1 = repository.resolve(rev1);
        	rc1 = rw.parseCommit(objId1);
        	objId2 = repository.resolve(rev2);
        	rc2 = rw.parseCommit(objId2);
        }
        else
        {
        	objId1 = repository.resolve(Constants.HEAD);
        	rc1 = rw.parseCommit(objId1);
        	objId2 = rc1.getParent(0).getId();
        	rc2 = rw.parseCommit(objId2);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();  
	    DiffFormatter df = new DiffFormatter(out); 
        df.setRepository(repository);
        df.setPathFilter(PathFilter.create(filePath));
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(true);
        List<DiffEntry> diffs = df.scan(rc1.getTree(), rc2.getTree());
        
        String diffText = null;
        for (DiffEntry diff : diffs) {
        	df.format(diff);
        	diffText = out.toString("utf-8");
        	System.out.println(diffText);
            System.out.println(MessageFormat.format("({0} {1} {2}", diff.getChangeType().name(), diff.getNewMode().getBits(), diff.getNewPath()));
        }
        
        del(new File((String)rstMap.get("dirStr")));
        
		return null;
	}
    
	//��ȡ�汾֮��ı��:����ĳ��Ŀ¼�ı�������ĳ���ļ��ı����Ϣ���������filePath��ȷ��
	public static List<GitDiffStatusDto> getChanges(Map<String, Object> rstMap, String gitRoot, String branchName, String rev1, String rev2, String filePath) throws Exception
	{	
		boolean isNew = false;
		if (rstMap == null)
		{	
			isNew = true;
			rstMap = getGit(gitRoot, branchName);
		}
        Git git = (Git)rstMap.get("git");
        Repository repository = git.getRepository();
        
        ObjectReader reader = repository.newObjectReader();  
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();  
		
		ObjectId old = null;
		ObjectId head = null;
		if (rev1 != null && rev2 != null)
		{
			old = repository.resolve(rev1 + "^{tree}");  
			head = repository.resolve(rev2+"^{tree}");
		}
		else
		{
			String rev = null;
			if (rev1 != null)
				rev = rev1;
			else
				rev = HEAD;
			
			old = repository.resolve(rev + "^{tree}");
			head = repository.resolve(rev + "^^{tree}");
			/*old = repository.resolve(HEAD + "^{tree}");
			head = repository.resolve(HEAD + "^^{tree}");*/
		}
		
		oldTreeIter.reset(reader, old);  
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();  
		newTreeIter.reset(reader, head);  
		
		List<DiffEntry> diffs = null;
		if (filePath != null)
		{
			diffs = git.diff().setPathFilter(PathFilter.create(filePath))
						.setNewTree(newTreeIter)  
						.setOldTree(oldTreeIter)  
						.call();
		}
		
		GitDiffStatusDto diffStatusDto = null;
		List<GitDiffStatusDto> rstList = null;
		DiffEntry diff = null;
		if (diffs != null && diffs.size() > 0)
		{	
			rstList = new ArrayList<GitDiffStatusDto>();
			for (Iterator<DiffEntry> iter = diffs.iterator(); iter.hasNext();)
			{
				diff = iter.next();
				ByteArrayOutputStream out = new ByteArrayOutputStream();  
			    DiffFormatter df = new DiffFormatter(out);  
			    df.setRepository(repository);  
			    df.format(diff);
			    String diffText = out.toString("gb2312");
			    System.out.println(diffText);
			    
				diffStatusDto = new GitDiffStatusDto(diff, gitRoot);
				rstList.add(diffStatusDto);
			}
		}
		
		if (isNew)
			del(new File((String)rstMap.get("dirStr")));
		
		return rstList;
	}
	
	//ĳ���ļ�ָ���汾������
    public static ByteArrayOutputStream getContent(String gitRoot, String branchName, String revision, String filePath) throws Exception
    {
    	Map<String, Object> rstMap = getGit(gitRoot, branchName);
        Git git = (Git)rstMap.get("git");
        Repository repository = git.getRepository();
		
		byte[] bytes = null;
		ByteArrayOutputStream  out = null;
		if (repository != null)
		{
			RevWalk walk = new RevWalk(repository);
			ObjectId objId = null;
			if (revision != null)
				objId = repository.resolve(revision);
			else
				objId = repository.resolve(HEAD);//���û�а汾�ţ���ȥHEAD
		    
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
		    //loader.copyTo(out);
		    if (loader != null)
		    	bytes = loader.getBytes();
		}
    	
		System.out.println(new String(bytes, "utf-8"));
		
		del(new File((String)rstMap.get("dirStr")));
		
		return out;
    }  
    
    public static List<GitDirEntryDto> listDirEntry(String gitRoot, String branchName, String revision, String filePath) throws Exception
    {
    	Map<String, Object> rstMap = getGit(gitRoot, branchName);
        Git git = (Git)rstMap.get("git");
        Repository repo = git.getRepository();
        
        List<String> dirList = null;
        List<GitDirEntryDto> rstDtoList = null;
        if (repo != null)
		{
        	ObjectId commitId = repo.resolve(revision);
            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(commitId);
            RevTree tree= commit.getTree();
            
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            if (filePath != null)
            	treeWalk.setFilter(PathFilter.create(filePath));            
            
            List<String> pathList = new ArrayList<String>();
            while (treeWalk.next())
            {	
            	pathList.add(treeWalk.getPathString());
            	System.out.println(treeWalk.getNameString() + ", " + treeWalk.getPathString());            	
            }
            
            String path = null;
            String dir = null;
            dirList = new ArrayList<String>();
            for (Iterator<String> iter = pathList.iterator(); iter.hasNext();)
            {
            	path = iter.next();
            	if (filePath != null)
            	{
            		String str = path.substring(filePath.length() + 1);
                	int idx = -1;
                	if (str.indexOf("/") == -1)
                		dir = filePath + "/" + str;
                	else
                	{
                		idx = str.indexOf("/");
                		dir = filePath + "/" + str.substring(0, idx);
                	}
            	}
            	else
            	{
            		if (path.indexOf("/") == -1)
            			dir = path;
            		else
            			dir = path.substring(0, path.indexOf("/"));
            	}
            	
            	if (!dirList.contains(dir))
            		dirList.add(dir);
            }
            
            rstDtoList = listDirEntry(rstMap, gitRoot, branchName, revision, dirList);
		}
        
        del(new File((String)rstMap.get("dirStr")));
        
        return rstDtoList;
    }
    
    private static List<GitDirEntryDto> listDirEntry(Map<String, Object> rstMap, String gitRoot, String branchName, String revision, List<String> dirList) throws Exception
    {
    	List<GitDirEntryDto> rstDtoList = null;
    	GitDirEntryDto entryDto = null;
    	GitDiffStatusDto diffDto = null;
    	GitLogDto logDto = null;
    	
    	Repository repo = ((Git)rstMap.get("git")).getRepository();
    	RevWalk rw = new RevWalk(repo);
    	
    	String filePath = null;
    	String name = null;
    	List<GitDiffStatusDto> diffDtoList = null;
    	List<GitLogDto> logDtoList = null;
    	if (dirList != null)
    	{
    		rstDtoList = new ArrayList<GitDirEntryDto>();
    		for (Iterator<String> iter = dirList.iterator(); iter.hasNext();)
    		{
    			filePath = iter.next();
    			if (filePath.indexOf("/") == -1)
    				name = filePath;
    			else
    				name = filePath.substring(filePath.lastIndexOf("/") + 1);
    			
    			diffDtoList = getChanges(rstMap, gitRoot, null, revision, null, filePath);
    			ObjectId objId = repo.resolve(revision);
            	RevCommit rev1 = rw.parseCommit(objId);
            	RevCommit rev2 = rw.parseCommit(rev1.getParent(0).getId());
            	logDtoList = getLog(rstMap, gitRoot, null, revision, rev2.getName(), filePath);
    			
            	entryDto = new GitDirEntryDto();
            	entryDto.name = name;
            	if (diffDtoList != null && diffDtoList.size() > 0)
            	{
            		diffDto = diffDtoList.get(0);
            		entryDto.kind = diffDto.kind;
        			entryDto.relativePath = diffDto.path;
        			entryDto.url = diffDto.url;
        			entryDto.size = diffDto.size;
        			entryDto.repositoryRoot = diffDto.repoRoot;
            	}
            	
            	if (logDtoList != null && logDtoList.size() > 0)
            	{	
            		logDto = logDtoList.get(0);
            		entryDto.commitAuthor = logDto.author;
        			entryDto.commitDate = logDto.commitDate;
        			entryDto.commitMessage = logDto.commitMessage;
        			entryDto.commitRevision = logDto.revision;
            	}

    			rstDtoList.add(entryDto);
    		}
    	}
    	
    	return rstDtoList;
    }
    
	/** 
     * ��ȡ��һ�汾�ı����¼��������������ļ���������ʾ����Ϊ���ع�ʱ����Ҫ�ع��������ļ� 
     * @param gitRoot git�ֿ�Ŀ¼ 
     * @param revision �汾�� 
     * @return 
     * @throws Exception 
     */  
    public static List<DiffEntry> rollBackFile(String gitRoot, String revision) throws Exception 
    {  
        Git git = Git.open(new File(gitRoot));  
        Repository repository = git.getRepository();  
  
        ObjectId objId = repository.resolve(revision);  
        Iterable<RevCommit> allCommitsLater = git.log().add(objId).call();  
        Iterator<RevCommit> iter = allCommitsLater.iterator();  
        RevCommit commit = iter.next();  
        TreeWalk tw = new TreeWalk(repository);  
        tw.addTree(commit.getTree());  
        commit = iter.next();  
        if (commit != null) {  
            tw.addTree(commit.getTree());  
        } else {  
            throw new Exception("��ǰ��ֻ��һ���汾�����ܻ�ȡ�����¼");  
        }  
  
        tw.setRecursive(true);  
        RenameDetector rd = new RenameDetector(repository);  
        rd.addAll(DiffEntry.scan(tw));  
        List<DiffEntry> diffEntries = rd.compute();  
        if (diffEntries == null || diffEntries.size() == 0) {  
            return diffEntries;  
        }  
        Iterator<DiffEntry> iterator = new ArrayList<DiffEntry>(diffEntries).iterator();  
        DiffEntry diffEntry = null;  
        while (iterator.hasNext()) {  
            diffEntry = iterator.next();  
            System.out.println("newPath:" + diffEntry.getNewPath() + "    oldPath:"  
                               + diffEntry.getOldPath() + "   changeType:"  
                               + diffEntry.getChangeType());  
            if (diffEntry.getChangeType() == ChangeType.DELETE) {  
                iterator.remove();  
            }  
        }
        
        return diffEntries;  
    }
    
    /** 
     * �ع���ָ���汾����һ���汾 
     * @param gitRoot git�ֿ�Ŀ¼ 
     * @param diffEntries ��Ҫ�ع����ļ� 
     * @param revision �汾�� 
     * @param remark ��ע 
     * @return 
     * @throws Exception 
     */  
    public static boolean rollBackPreRevision(String gitRoot, List<DiffEntry> diffEntries, String revision, String remark) throws Exception
    {  
		if (diffEntries == null || diffEntries.size() == 0) {  
			throw new Exception("û����Ҫ�ع����ļ�");  
		}  
		
		Git git = Git.open(new File(gitRoot));  
		
		List<String> files = new ArrayList<String>();  
		
		//ע�⣺�����reset����Ὣ�ݴ��������ݻָ���ָ����revesion����״̬���൱��ȡ��add����Ĳ���  
		/*Repository repository = git.getRepository(); 
		
		RevWalk walk = new RevWalk(repository); 
		ObjectId objId = repository.resolve(revision); 
		RevCommit revCommit = walk.parseCommit(objId); 
		String preVision = revCommit.getParent(0).getName(); 
		ResetCommand resetCmd = git.reset(); 
		for (String file : files) { 
		resetCmd.addPath(file); 
		} 
		resetCmd.setRef(preVision).call(); 
		repository.close();*/  
		
		//ȡ����Ҫ�ع����ļ����������ļ����ع�  
		for (DiffEntry diffEntry : diffEntries) 
		{  
			if (diffEntry.getChangeType() == ChangeType.DELETE) 
			{  
				continue;  
			}
			else
			{  
				files.add(diffEntry.getNewPath());  
			}  
		}  
		
		if (files.size() == 0)
		{  
			throw new Exception("û����Ҫ�ع����ļ�");  
		}  
		
		//checkout�����ᶪʧ�����������ݣ��ݴ����͹����������ݻ�ָ���ָ����revision���İ汾����  
		CheckoutCommand checkoutCmd = git.checkout();  
		for (String file : files) 
		{  
			checkoutCmd.addPath(file);  
		}  
		//���ˡ�^����ʾָ���汾��ǰһ���汾�����û����һ�汾�����������лᱨ�����磺error: pathspec '4.vm' did not match any file(s) known to git.  
		checkoutCmd.setStartPoint(revision + "^");  
		checkoutCmd.call();  
		
		//�����ύһ��  
		CommitCommand commitCmd = git.commit();  
		for (String file : files) 
		{  
			commitCmd.setOnly(file);  
		}
		
		commitCmd.setCommitter("yonge", "654166020@qq.com").setMessage(remark).call();  
		
		return true;  
	}
    
    
    
}
