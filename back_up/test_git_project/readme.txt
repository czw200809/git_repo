finished:
	1.//��ȡlog,���revision = null, ���ʾ��ȡ���а汾��log, �����ȡָ���汾��log
		public List<GitLogDto> getLog(String gitRoot, String revision) throws Exception
	2.//�Ƚ������汾֮��Ĳ���(ĳ���ļ���ȫ��)
		public List<String> getDiffBetweenRevisions(String gitRoot, String Child, String Parent, String filePath) throws Exception
	3.//ĳ���ļ�ָ���汾������
    	public byte[] getContentWithSpecifiedRevision(Repository repository, String gitRoot, String revision, String filePath) throws Exception
      //��ȡ��ǰ�ļ���һ���汾������
    	public byte[] getPreRevisionContent(String gitRoot, String revision, String filePath) throws Exception
