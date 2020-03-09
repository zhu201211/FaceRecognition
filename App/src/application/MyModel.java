package application;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.CvType;

public class MyModel {
    	public int person=0;
        public Mat meanFaceMat=null;//ƽ��ֵ����
	public Mat m_matImage = null; //��Ųɼ��׶εĹ�һ��ǰ��ͼ�����ʶ��׶εĲ���ͼ��ľ���
	public Mat m_matNormImage = null; //ͼ��ɼ��׶λ���ʶ��׶ι�һ���Ժ������ͼ�����
	public Mat m_matTrainingImage = null; //ѵ������������M*N
	public Mat m_matEigenFace = null; //����������(�����)
	public Mat m_matEigenTrainingSamples = null; //ѵ�����������������ռ��ͶӰ����(�����)
	public Mat testFaceMat=null;
	public Mat SimilarImage=null;
	int numPerson; //�˵ĸ���
	int numSamplePerPerson; //ÿ���˵�ѵ��������
	int img_h,img_w;
	
	MyModel(){
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		numPerson = 10; //10����
		numSamplePerPerson = 5; //ÿ��5������
	}
	
	/**����ѵ����������������m_matTrainingImage������*/
	public void ReadTrainingSampleFiles(File file) { //fileΪѵ���������е�һ����������
		String fileParent = "file:///" + file.getParent();//���ѵ��������Ŀ¼
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); //ʹ�ñ����ļ���URL·��
		fileParent = fileParent.replace('\\', '/'); //ʹ�ñ����ļ���URL·��
		Image sample = new Image(filePath); //����һ��ѵ������
		//����ѵ����������numPerson*numSamplePerPerson�У�sample.getWidth()*sample.getHeight()��
		img_w = (int)sample.getWidth(); //ѵ���������
		img_h = (int)sample.getHeight(); //ѵ�������߶�
		m_matTrainingImage = new Mat(numPerson*numSamplePerPerson, img_w*img_h, CvType.CV_32F);
		for (int i=1; i<=numPerson; i++) {//��i����
			for (int j=1; j<=numSamplePerPerson; j++) //��j������
			{	
				float[] gray = new float[img_w*img_h]; //���1����ͼ���������صĻҶ�ֵ
				String s = fileParent+"/s"+i+"_"+j+".bmp"; //ͼ��si_j.bmp·��
				Image si_j = new Image(s);//��ȡͼ��si_j.bmp
				PixelReader si_jPixelReader = si_j.getPixelReader(); //ͼ��si_j.bmp���ض�����
				int n=0;
				for (int x=0; x<img_w; x++){ //���б���ͼ��
					for (int y=0; y<img_h; y++) {
						Color color = si_jPixelReader.getColor(x, y);
						gray[n] = (float)((color.getBlue()+color.getGreen()+color.getRed())/3.0);
						n++;
					}
				}
				m_matTrainingImage.put(((i-1)*numSamplePerPerson+j-1), 0, gray);
			}
		}
	}
	
	/**��ѵ������������m_matTrainingImage�ĵõ����������󣬱�����������m_matEigenFace��*/
	public void CalculateEigenFaceMat() {
	    
		meanFaceMat=new Mat();//ƽ��ֵ����
		
		Mat normTrainFaceMat=new Mat(m_matTrainingImage.height(),m_matTrainingImage.width(),CvType.CV_32F);//��񻯺����
		Mat temp1=new Mat(m_matTrainingImage.height(),m_matTrainingImage.width(),CvType.CV_32F);
		//����ѵ��������ƽ��ֵ����
		Core.reduce(m_matTrainingImage, meanFaceMat, 0, Core.REDUCE_AVG);
		System.out.println(meanFaceMat.dump());
		System.out.println(m_matTrainingImage.height());
		//��ֵ
		for (int i = 0; i <m_matTrainingImage.height(); i++) {
		    for (int j = 0; j < meanFaceMat.width(); j++) {
			temp1.put(i, j, meanFaceMat.get(0, j));
		    }
		}
		//��������õ���񻯺�ľ���normTrainFaceMat
		Core.subtract(m_matTrainingImage, temp1, normTrainFaceMat);
		//System.out.println("normTrainFaceMat="+normTrainFaceMat.dump());
		
		Mat temp2=new Mat();
		Mat temp3=new Mat();
		//����ת��
		Core.transpose(normTrainFaceMat, temp2);//temp2=normTrainFaceMat ' 
		//������� 
		Core.gemm(normTrainFaceMat, temp2, 1, new Mat(), 0, temp3);//temp3=normTrainFaceMat* normTrainFaceMat��
		Mat temp4=new Mat();//����ֵ
		Mat temp5=new Mat();//��������
		Core.eigen(temp3, temp4,temp5);
		//System.out.println(temp4.dump());
		//System.out.println("temp5="+temp5.dump());
		
		double sum_temp4=Core.norm(temp4,Core.NORM_L1);//����ֵ֮��
		int m=0;
		double sum=0;
                for(int i=0;i<temp4.height();i++)
                {
           	    if(sum>0.9*sum_temp4)
           	    	   break;
           	    sum+=temp4.get(i, 0)[0];
           	    m++;
                }
                //��ȡ����������ǰm��
                System.out.println("m="+m);
               //��ά����
               for(int i=0;i<m;i++)
               {
           	     Mat x=temp5.col(i);
           	     Core.divide(x, Scalar.all(Math.sqrt(temp4.get(i,0)[0])),temp5.col(i));//x/sqrt(sort_value(i))
                }
               temp5=new Mat(temp5, new Range(0,temp5.height()), new Range(0,m));//�������䣬����Ϊm
               
               m_matEigenFace=new Mat();
               m_matEigenTrainingSamples=new Mat();
               
		//���ѵ������������������m_matEigenFace
               Core.gemm(temp2, temp5, 1, new Mat(), 0, m_matEigenFace);//m_matEigenFace = normTrainFaceMat' * temp5
		//ѵ���������������ռ��ͶӰ����m_matEigenTrainingSamples
               Core.gemm(normTrainFaceMat, m_matEigenFace, 1, new Mat(), 0, m_matEigenTrainingSamples);
            
	}
	
	/**�ú�����m_matImage����˫��������м��κͻҶȹ�һ�������������m_matNormImage�� */
	public void NormalizeImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
	    	int O_x,O_y;
		int d,min_x,min_y;
		double size;
		int h,w,x,y;
		O_x=righteye_x-lefteye_x;
		O_y=righteye_y-lefteye_y;
		min_x=(int)((lefteye_x+righteye_x)/2);//��ֻ�۾����е�����
		min_y=(int)((lefteye_y+righteye_y)/2);
		
		d=(int)(Math.sqrt(Math.pow(O_x, 2)+Math.pow(O_y, 2)));//���۾�����
		size=(double)(2.0*d/128);//����
		x=(int)(min_x-d);
		y=(int)(min_y-d/2);
		w=(int)(2*d);
		h=(int)(2*d);
		if (x<0) {
		    x=0;
		}
		if (y<0) {
		    y=0;
		}
		if(w>m_matImage.width()){
		    w=m_matImage.width();
		}
		if(h>m_matImage.height()){
		    h=m_matImage.height();
		}
		
		Rect r=new Rect(x,y,w,h);
		
		Mat m=new Mat(m_matImage,r);
		m_matNormImage=new Mat();
		Imgproc.resize(m, m_matNormImage, new Size(w,h));
		Imgproc.resize(m_matNormImage, m_matNormImage, new Size(48,48));//������СΪ48*48
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_8UC1,255);
		Imgproc.equalizeHist(m_matNormImage, m_matNormImage);//ֱ��ͼ���⻯
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_32F,1.0/255);
		
	}
	
	/**��������m_matTrainingImage�����Զ������ļ���ʽ�����ڵ�ǰ�ļ�����*/
	public void Savem_matTrainingImage() {
		//ʹ��java�Ķ������������ļ�����
	    String str="m_matTrainingImage";
	    String fileName=str+".txt";
	    try{  
	            DataOutputStream out=new DataOutputStream( new BufferedOutputStream(new FileOutputStream(fileName))); 
	            out.writeInt(m_matTrainingImage.height());
	           // System.out.println(meanFaceMat.width());
	            out.writeInt(m_matTrainingImage.width());
	            out.writeChar('\n');
	            for(int i=0;i<m_matTrainingImage.height();i++)
	            {
	            	for(int j=0;j<m_matTrainingImage.width();j++)
	            	{
	            		out.writeDouble(m_matTrainingImage.get(i, j)[0]); 
	            	}
	            	out.writeChar('\n');
	            }  
	            out.writeDouble(-1);
	            out.close();  
	        } catch (Exception e)  
	        {  
	            e.printStackTrace();  
	        }  
	    System.out.println("�ɹ�����m_matTrainingImage.txt");
	}
	
	/**�ӵ�ǰĿ¼����ѵ����������Ķ������ļ����������뵽m_matTrainingImage�ļ���*/
	@SuppressWarnings({ "unused", "resource" })
	public void Readm_matTrainingImageFromFile() throws IOException
	{
	    	String fileName="m_matTrainingImage.txt";
	   	double a;
		int i=0,j=0;
		int row,col;
		Mat m=null;
                try   {  
                    DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));  
                    row=in.readInt();
                    col=in.readInt();
            	   char c=in.readChar();
            	   m=new Mat(row,col,CvType.CV_32F);
                    while((a=in.readDouble())!=-1)  {
                		m.put(i, j, a);
                		j++;
                		if(j==col) {
                			c=in.readChar();
                        		i++;
                        		j=0;
                		}
                    }
                } catch (Exception e)  {  
                    e.printStackTrace();  
                } 
                m_matTrainingImage=m;
	}
	
	/**��������meanFaceMat�����Զ������ļ���ʽ�����ڵ�ǰ�ļ�����*/
	public void SavemeanFaceMat() {
		//ʹ��java�Ķ������������ļ�����
	    String str="meanFaceMat";
	    String fileName=str+".txt";
	    try{  
	            DataOutputStream out=new DataOutputStream( new BufferedOutputStream(new FileOutputStream(fileName))); 
	            out.writeInt(meanFaceMat.height());
	           // System.out.println(meanFaceMat.width());
	            out.writeInt(meanFaceMat.width());
	            out.writeChar('\n');
	            for(int i=0;i<meanFaceMat.height();i++)
	            {
	            	for(int j=0;j<meanFaceMat.width();j++)
	            	{
	            		out.writeDouble(meanFaceMat.get(i, j)[0]); 
	            	}
	            	out.writeChar('\n');
	            }  
	            out.writeDouble(-1);
	            out.close();  
	        } catch (Exception e)  
	        {  
	            e.printStackTrace();  
	        }  
	    System.out.println("�ɹ�����meanFaceMat.txt");
	}
	
	/**�ӵ�ǰĿ¼����ƽ������Ķ������ļ����������뵽meanFaceMat�ļ���*/
	@SuppressWarnings({ "unused", "resource" })
	public void ReadmeanFaceMatFromFile() throws IOException
	{
	    	String fileName="meanFaceMat.txt";
	   	double a;
		int i=0,j=0;
		int row,col;
		Mat m=null;
                try   {  
                    DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));  
                    row=in.readInt();
                    col=in.readInt();
                    System.out.println(col);
                    img_h=img_w=(int)Math.sqrt(col);
            	   char c=in.readChar();
            	   m=new Mat(row,col,CvType.CV_32F);
                    while((a=in.readDouble())!=-1)  {
                		m.put(i, j, a);
                		j++;
                		if(j==col) {
                			c=in.readChar();
                        		i++;
                        		j=0;
                		}
                    }
                } catch (Exception e)  {  
                    e.printStackTrace();  
                } 
                meanFaceMat=m;
	}
	
	
	/**��������m_matEigenFace�����Զ������ļ���ʽ�����ڵ�ǰ�ļ�����*/
	public void SaveEigenFaceMat() {
		//ʹ��java�Ķ������������ļ�����
	    String str="m_matEigenFace";
	    String fileName=str+".txt";
	    try{  
	            DataOutputStream out=new DataOutputStream( new BufferedOutputStream(new FileOutputStream(fileName))); 
	            out.writeInt(m_matEigenFace.height());
	            out.writeInt(m_matEigenFace.width());
	            out.writeChar('\n');
	            for(int i=0;i<m_matEigenFace.height();i++)
	            {
	            	for(int j=0;j<m_matEigenFace.width();j++)
	            	{
	            		out.writeDouble(m_matEigenFace.get(i, j)[0]); 
	            	}
	            	out.writeChar('\n');
	            }  
	            out.writeDouble(-1);
	            out.close();  
	        } catch (Exception e)  
	        {  
	            e.printStackTrace();  
	        }  

	   	 //System.out.println(m_matEigenTrainingSamples.dump());
	    	System.out.println("�ɹ�����m_matEigenFace.txt");
	}

	
	/**�ӵ�ǰĿ¼��������������Ķ������ļ����������뵽m_matEigenFace�ļ���*/
	@SuppressWarnings({ "unused", "resource" })
	public void ReadEigenFaceMatFromFile() throws IOException
	{
	    	String fileName="m_matEigenFace.txt";
	   	double a;
		int i=0,j=0;
		int row,col;
		Mat m=null;
                try   {  
                    DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));  
                    row=in.readInt();
                   
                    col=in.readInt();
            	   char c=in.readChar();
            	   m=new Mat(row,col,CvType.CV_32F);
                    while((a=in.readDouble())!=-1)  {
                		m.put(i, j, a);
                		j++;
                		if(j==col) {
                			c=in.readChar();
                        		i++;
                        		j=0;
                		}
                    }
                } catch (Exception e)  {  
                    e.printStackTrace();  
                } 
		m_matEigenFace=m;
	}
	
	/**��������m_matEigenTrainingSamples�����Զ������ļ���ʽ�����ڵ�ǰ�ļ�����*/
	public void SaveEigenTrainingSamplesMat() {
		//ʹ��java�Ķ������������ļ�����
	    String str="m_matEigenTrainingSamples";
	    String fileName=str+".txt";
	    try {  
	            DataOutputStream out=new DataOutputStream(new BufferedOutputStream( new FileOutputStream(fileName))); 
	            int i,j;
	            out.writeInt(m_matEigenTrainingSamples.height());
	            out.writeInt(m_matEigenTrainingSamples.width());
	            out.writeChar('\n');
	            for(i=0;i<m_matEigenTrainingSamples.height();i++)
	            {
	            	for(j=0;j<m_matEigenTrainingSamples.width();j++)
	            	{
	            		out.writeDouble(m_matEigenTrainingSamples.get(i, j)[0]); 
	            	}
	            	out.writeChar('\n');
	            }  
	            out.writeDouble(-1);
	            out.close();  
	        } catch (Exception e)  
	        {  
	            e.printStackTrace();  
	        }
	    
	    	System.out.println("�ɹ�����m_matEigenTrainingSamples.txt");
	}
	
	/**�ӵ�ǰĿ¼����ѵ�����������������ռ��ͶӰ����Ķ������ļ����������뵽m_matEigenTrainingSamples�ļ���*/
	@SuppressWarnings({ "resource", "unused" })
	public void ReadEigenTrainingSamplesMatFromFile() throws IOException
	{
	    String fileName="m_matEigenTrainingSamples.txt";
	    double a;
	    int i=0,j=0;
            int row,col;
	    Mat m=null;
            try  {  
                DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));  
                row=in.readInt();
                col=in.readInt();
        	char c=in.readChar();
        	m=new Mat(row,col,CvType.CV_32F);
                while((a=in.readDouble())!=-1) {
            		m.put(i, j, a);
            		j++;
            		if(j==col){
            			c=in.readChar();
                		i++;
                		j=0;
            		}
                }
            } catch (Exception e) {  
                e.printStackTrace();  
            } 
            m_matEigenTrainingSamples=m;
		
	}
	
	/**������ӽ�������������������:ʹ�ò���������m_matNormImage����������������m_matEigenFace���м���*/
	/**�ú������������������ӽ�����������*/
	@FXML
	public Mat GetSimilarFaceMat(){
	    person=0;
	    SimilarImage=new Mat(img_w,img_h,CvType.CV_32F);
	    if (m_matNormImage==null) {
		m_matNormImage=m_matImage;
	    }
	    Imgproc.resize(m_matNormImage, m_matNormImage, new Size(img_h,img_w));//������ͼ��ת����������С
	    Mat m_normTestFaceMat =new Mat(1,m_matNormImage.height()*m_matNormImage.width(),CvType.CV_32F);
	    //��������չ��Ϊ1��N����testFaceMat
	    testFaceMat=new Mat(1,m_matNormImage.height()*m_matNormImage.width(),CvType.CV_32F);
	    
	    float[] gray = new float[img_w*img_h]; //���1����ͼ���������صĻҶ�ֵ
	    
	    WritableImage testImage = GetImageFromMatrix(m_matNormImage);//��ȡ�򿪵Ĳ���ͼ��
	    
	    PixelReader si_jPixelReader = testImage.getPixelReader(); //ͼ��si_j.bmp���ض�����
	    int n=0;
	  //����ԭͼ��ÿ�����أ�����д�뵽������������
	    for (int x=0; x<img_w; x++) { //���б���ͼ��
		for (int y=0; y<img_h; y++) {
			Color color = si_jPixelReader.getColor(x, y);
			gray[n] = (float)((color.getBlue()+color.getGreen()+color.getRed())/3.0);
			n++;
		}
	    }
	    testFaceMat.put(0, 0, gray);//��ò�������1��N����
		
	    Core.subtract(testFaceMat, meanFaceMat, m_normTestFaceMat);//���
		 
	    System.out.printf("%d %d  %d %d\n",m_normTestFaceMat.height(),m_normTestFaceMat.width(),m_matEigenFace.height(),m_matEigenFace.width());
	    Mat eigen_test_sample=new Mat();//��ʶ��������ͶӰ��������
	    //eigen_test_sample = normTestFaceMat * m_matEigenFace
	    Core.gemm(m_normTestFaceMat, m_matEigenFace, 1, new Mat(), 0, eigen_test_sample);
	    
	    //System.out.println(eigen_test_sample.dump());
	    double min=100,f;
	    int index=0;
	    for(int i=0;i<m_matEigenTrainingSamples.height();i++)
	    {
		f=Core.norm(eigen_test_sample, m_matEigenTrainingSamples.row(i), Core.NORM_L2);
		System.out.printf("%d,%f\n",i,f);
		if(f<min)
		{
		    min=f;
		    index=i;
		}
	    }
	    if (min>0&&min<8) {//��ֵ��Χ0-10
        	   Mat tureMat=m_matTrainingImage.row(index);//��ȡ��ѵ�������ж�Ӧ��ͼ�����1XN
        	   System.out.println(tureMat.dump());
        	   System.out.println(tureMat.rows()+","+tureMat.cols());
        	   //ת��Ϊͼ���Сimg_w*img_h�ľ���
        	   int a=0,b=0;
        	   for (int i = 0; i <img_w*img_h ; i++) {
        	       double s=tureMat.get(0, i)[0];
        	       //System.out.println(s);
        	       SimilarImage.put(a, b, s);
        	       a++;
        	       if (a==img_h) {
        		a=0;b++;
        	       }
        	   }
        	   
        	   System.out.println(index+1+","+min);
        	   String str="";
        	   if (((int)((index+1)%numSamplePerPerson))!=0) {
        	       person=(index+1)/numSamplePerPerson+1;
        	       str="s"+String.valueOf((index+1)/numSamplePerPerson+1)+"_"+String.valueOf((index+1)%numSamplePerPerson)+".bmp";
        	   }	else{
        	       person=(index+1)/numSamplePerPerson;
        	       str="s"+String.valueOf((index+1)/numSamplePerPerson)+"_"+String.valueOf((index+1)%numSamplePerPerson+numSamplePerPerson)+".bmp";
        	   }
        	   System.out.println("�����Ƶ�ͼ��:"+str);
            	  
        	   return SimilarImage;	
	    }
	    else{
		System.out.println(index+1+","+min);
		String str="";
     	   	if (((int)((index+1)%numSamplePerPerson))!=0) 
     	   	    str="s"+String.valueOf((index+1)/numSamplePerPerson+1)+"_"+String.valueOf((index+1)%numSamplePerPerson)+".bmp";
     	   	else
     	   	    str="s"+String.valueOf((index+1)/numSamplePerPerson)+"_"+String.valueOf((index+1)%numSamplePerPerson+numSamplePerPerson)+".bmp";
     	   	System.out.println(""+str);
     	   	
    		System.out.println("����û�д��ˣ�");
    		return null;
	    }
	}
	
	/**������matImg�е�ֵ��ͼ��ķ�ʽ���*/
	public WritableImage GetImageFromMatrix(Mat matImg) {
		if (matImg != null) {
			WritableImage wImage = new WritableImage(matImg.width(), matImg.height());
			//�õ�����д����
			PixelWriter pixelWriter = wImage.getPixelWriter();
			if (matImg.channels() == 1) {  //��ͨ��ͼ��
				float[] gray = new float[matImg.height()*matImg.width()];
				matImg.get(0, 0, gray);
				//����ͼ�����ÿ�����أ�����д�뵽Ŀ��ͼ��
				for (int y=0; y<matImg.height(); y++) {
					for (int x=0; x<matImg.width(); x++) {
						int pixelIndex = y*matImg.width()+x;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}	
			}
			else if (matImg.channels() == 3) {	//3ͨ��ͼ��
				//����Դͼ��ÿ�����أ�����д�뵽Ŀ��ͼ��
				for (int y=0; y<matImg.height(); y++) {
					for (int x=0; x<matImg.width(); x++) {
						int[] gray = new int[3];
						matImg.get(y, x, gray);
						Color color = Color.rgb(gray[2], gray[1], gray[0]);
						pixelWriter.setColor(x, y, color);
					}
				}	
			}
			return wImage;
		}
		return null;
	}
	
	/**��ͼ��image�����������m_matImage������*/
	public void GetGrayImgMatFromImage(Image image) {
		if (image != null) {
			PixelReader pixelReader = image.getPixelReader();
			m_matImage = new Mat((int)(image.getHeight()),(int)(image.getWidth()), CvType.CV_32F);
			//����ԭͼ��ÿ�����أ�����д�뵽Ŀ��ͼ�����
			for (int y=0; y<image.getHeight(); y++) {
				for (int x=0; x<image.getWidth(); x++) {
					Color color = pixelReader.getColor(x, y);
					float gray = (float)((color.getBlue()+color.getGreen()+color.getRed())/3.0);
					m_matImage.put(y, x, gray);
				}
			}
		}
	}
	
	/**�ӻҶ�ͼ�񼯺�(ѵ��������������������)�����г�ȡһ�л�һ����ɶ�άͼ��*/
	@SuppressWarnings("null")
	public WritableImage GetImageFromGrayImagesMatrix(Mat matImages, //ͼ�񼯺Ͼ���
												  	int imgWidth, //��άͼ��Ŀ��
												  	int index,    //ȡ���л����е�����
												  	boolean byRow) //trueΪ��ȡһ�У�falseΪ��ȡһ��
	{
		if (matImages != null || matImages.channels() != 1) {
			int widthMat = matImages.width();
			int heightMat = matImages.height();
			if (byRow == true) { //���г�ȡ
				int imgHeight = widthMat/imgWidth; //��άͼ��߶�
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				//�õ�����д����
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[widthMat];
				matImages.get(index, 0, gray);
				//����ͼ��ÿ������
				for (int x=0; x<wImage.getWidth(); x++) {
					for (int y=0; y<wImage.getHeight(); y++) {
						int pixelIndex = x*imgHeight+y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}	
				return wImage;
			}
			else { //���г�ȡ
				int imgHeight = heightMat/imgWidth; //��άͼ��߶�
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				//�õ�����д����
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[heightMat];
				Mat matTImages = matImages.t(); //����ת��
				matTImages.get(index, 0, gray);
				//����ͼ��ÿ������
				for (int x=0; x<wImage.getWidth(); x++) {
					for (int y=0; y<wImage.getHeight(); y++) {
						int pixelIndex = x*imgHeight+y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}
				return wImage;
			}
		}
		return null;
	}
}
