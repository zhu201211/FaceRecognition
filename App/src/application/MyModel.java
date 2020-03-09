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
        public Mat meanFaceMat=null;//平均值矩阵
	public Mat m_matImage = null; //存放采集阶段的归一化前的图像或者识别阶段的测试图像的矩阵
	public Mat m_matNormImage = null; //图像采集阶段或者识别阶段归一化以后的人脸图像矩阵
	public Mat m_matTrainingImage = null; //训练样本集矩阵M*N
	public Mat m_matEigenFace = null; //特征脸矩阵(需存盘)
	public Mat m_matEigenTrainingSamples = null; //训练样本集在特征脸空间的投影矩阵(需存盘)
	public Mat testFaceMat=null;
	public Mat SimilarImage=null;
	int numPerson; //人的个数
	int numSamplePerPerson; //每个人的训练样本数
	int img_h,img_w;
	
	MyModel(){
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		numPerson = 10; //10个人
		numSamplePerPerson = 5; //每人5个样本
	}
	
	/**读入训练样本集，保存在m_matTrainingImage矩阵中*/
	public void ReadTrainingSampleFiles(File file) { //file为训练样本集中的一个样本对象
		String fileParent = "file:///" + file.getParent();//获得训练集所在目录
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); //使用本地文件的URL路径
		fileParent = fileParent.replace('\\', '/'); //使用本地文件的URL路径
		Image sample = new Image(filePath); //读入一个训练样本
		//创建训练样本矩阵：numPerson*numSamplePerPerson行，sample.getWidth()*sample.getHeight()列
		img_w = (int)sample.getWidth(); //训练样本宽度
		img_h = (int)sample.getHeight(); //训练样本高度
		m_matTrainingImage = new Mat(numPerson*numSamplePerPerson, img_w*img_h, CvType.CV_32F);
		for (int i=1; i<=numPerson; i++) {//第i个人
			for (int j=1; j<=numSamplePerPerson; j++) //第j个样本
			{	
				float[] gray = new float[img_w*img_h]; //存放1个人图像所有像素的灰度值
				String s = fileParent+"/s"+i+"_"+j+".bmp"; //图像si_j.bmp路径
				Image si_j = new Image(s);//获取图像si_j.bmp
				PixelReader si_jPixelReader = si_j.getPixelReader(); //图像si_j.bmp像素读入器
				int n=0;
				for (int x=0; x<img_w; x++){ //按列遍历图像
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
	
	/**由训练集样本矩阵：m_matTrainingImage的得到特征脸矩阵，保存在数据域m_matEigenFace中*/
	public void CalculateEigenFaceMat() {
	    
		meanFaceMat=new Mat();//平均值矩阵
		
		Mat normTrainFaceMat=new Mat(m_matTrainingImage.height(),m_matTrainingImage.width(),CvType.CV_32F);//规格化后矩阵
		Mat temp1=new Mat(m_matTrainingImage.height(),m_matTrainingImage.width(),CvType.CV_32F);
		//计算训练样本的平均值矩阵
		Core.reduce(m_matTrainingImage, meanFaceMat, 0, Core.REDUCE_AVG);
		System.out.println(meanFaceMat.dump());
		System.out.println(m_matTrainingImage.height());
		//赋值
		for (int i = 0; i <m_matTrainingImage.height(); i++) {
		    for (int j = 0; j < meanFaceMat.width(); j++) {
			temp1.put(i, j, meanFaceMat.get(0, j));
		    }
		}
		//矩阵相减得到规格化后的矩阵normTrainFaceMat
		Core.subtract(m_matTrainingImage, temp1, normTrainFaceMat);
		//System.out.println("normTrainFaceMat="+normTrainFaceMat.dump());
		
		Mat temp2=new Mat();
		Mat temp3=new Mat();
		//矩阵转置
		Core.transpose(normTrainFaceMat, temp2);//temp2=normTrainFaceMat ' 
		//矩阵相乘 
		Core.gemm(normTrainFaceMat, temp2, 1, new Mat(), 0, temp3);//temp3=normTrainFaceMat* normTrainFaceMat’
		Mat temp4=new Mat();//特征值
		Mat temp5=new Mat();//特征向量
		Core.eigen(temp3, temp4,temp5);
		//System.out.println(temp4.dump());
		//System.out.println("temp5="+temp5.dump());
		
		double sum_temp4=Core.norm(temp4,Core.NORM_L1);//特征值之和
		int m=0;
		double sum=0;
                for(int i=0;i<temp4.height();i++)
                {
           	    if(sum>0.9*sum_temp4)
           	    	   break;
           	    sum+=temp4.get(i, 0)[0];
           	    m++;
                }
                //获取特征向量的前m列
                System.out.println("m="+m);
               //降维处理
               for(int i=0;i<m;i++)
               {
           	     Mat x=temp5.col(i);
           	     Core.divide(x, Scalar.all(Math.sqrt(temp4.get(i,0)[0])),temp5.col(i));//x/sqrt(sort_value(i))
                }
               temp5=new Mat(temp5, new Range(0,temp5.height()), new Range(0,m));//行数不变，列数为m
               
               m_matEigenFace=new Mat();
               m_matEigenTrainingSamples=new Mat();
               
		//获得训练样本的特征脸矩阵m_matEigenFace
               Core.gemm(temp2, temp5, 1, new Mat(), 0, m_matEigenFace);//m_matEigenFace = normTrainFaceMat' * temp5
		//训练样本在特征脸空间的投影矩阵m_matEigenTrainingSamples
               Core.gemm(normTrainFaceMat, m_matEigenFace, 1, new Mat(), 0, m_matEigenTrainingSamples);
            
	}
	
	/**该函数将m_matImage根据双眼坐标进行几何和灰度归一化，结果保存在m_matNormImage中 */
	public void NormalizeImage(int lefteye_x, int lefteye_y, int righteye_x, int righteye_y) {
	    	int O_x,O_y;
		int d,min_x,min_y;
		double size;
		int h,w,x,y;
		O_x=righteye_x-lefteye_x;
		O_y=righteye_y-lefteye_y;
		min_x=(int)((lefteye_x+righteye_x)/2);//两只眼睛的中点坐标
		min_y=(int)((lefteye_y+righteye_y)/2);
		
		d=(int)(Math.sqrt(Math.pow(O_x, 2)+Math.pow(O_y, 2)));//两眼睛距离
		size=(double)(2.0*d/128);//倍数
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
		Imgproc.resize(m_matNormImage, m_matNormImage, new Size(48,48));//调整大小为48*48
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_8UC1,255);
		Imgproc.equalizeHist(m_matNormImage, m_matNormImage);//直方图均衡化
		m_matNormImage.convertTo(m_matNormImage, CvType.CV_32F,1.0/255);
		
	}
	
	/**将数据域m_matTrainingImage矩阵以二进制文件方式保存在当前文件夹中*/
	public void Savem_matTrainingImage() {
		//使用java的二进制流进行文件保存
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
	    System.out.println("成功保存m_matTrainingImage.txt");
	}
	
	/**从当前目录读入训练样本矩阵的二进制文件，将它存入到m_matTrainingImage文件中*/
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
	
	/**将数据域meanFaceMat矩阵以二进制文件方式保存在当前文件夹中*/
	public void SavemeanFaceMat() {
		//使用java的二进制流进行文件保存
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
	    System.out.println("成功保存meanFaceMat.txt");
	}
	
	/**从当前目录读入平均矩阵的二进制文件，将它存入到meanFaceMat文件中*/
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
	
	
	/**将数据域m_matEigenFace矩阵以二进制文件方式保存在当前文件夹中*/
	public void SaveEigenFaceMat() {
		//使用java的二进制流进行文件保存
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
	    	System.out.println("成功保存m_matEigenFace.txt");
	}

	
	/**从当前目录读入特征脸矩阵的二进制文件，将它存入到m_matEigenFace文件中*/
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
	
	/**将数据域m_matEigenTrainingSamples矩阵以二进制文件方式保存在当前文件夹中*/
	public void SaveEigenTrainingSamplesMat() {
		//使用java的二进制流进行文件保存
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
	    
	    	System.out.println("成功保存m_matEigenTrainingSamples.txt");
	}
	
	/**从当前目录读入训练样本集在特征脸空间的投影矩阵的二进制文件，将它存入到m_matEigenTrainingSamples文件中*/
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
	
	/**计算最接近测试人脸的样本人脸:使用测试人脸的m_matNormImage矩阵与特征脸矩阵m_matEigenFace进行计算*/
	/**该函数返回与测试人脸最接近的样本矩阵*/
	@FXML
	public Mat GetSimilarFaceMat(){
	    person=0;
	    SimilarImage=new Mat(img_w,img_h,CvType.CV_32F);
	    if (m_matNormImage==null) {
		m_matNormImage=m_matImage;
	    }
	    Imgproc.resize(m_matNormImage, m_matNormImage, new Size(img_h,img_w));//将现有图像转换成样本大小
	    Mat m_normTestFaceMat =new Mat(1,m_matNormImage.height()*m_matNormImage.width(),CvType.CV_32F);
	    //测试人脸展开为1×N矩阵testFaceMat
	    testFaceMat=new Mat(1,m_matNormImage.height()*m_matNormImage.width(),CvType.CV_32F);
	    
	    float[] gray = new float[img_w*img_h]; //存放1个人图像所有像素的灰度值
	    
	    WritableImage testImage = GetImageFromMatrix(m_matNormImage);//获取打开的测试图像
	    
	    PixelReader si_jPixelReader = testImage.getPixelReader(); //图像si_j.bmp像素读入器
	    int n=0;
	  //遍历原图像每个像素，将其写入到测试人脸矩阵
	    for (int x=0; x<img_w; x++) { //按列遍历图像
		for (int y=0; y<img_h; y++) {
			Color color = si_jPixelReader.getColor(x, y);
			gray[n] = (float)((color.getBlue()+color.getGreen()+color.getRed())/3.0);
			n++;
		}
	    }
	    testFaceMat.put(0, 0, gray);//获得测试人脸1×N矩阵
		
	    Core.subtract(testFaceMat, meanFaceMat, m_normTestFaceMat);//规格化
		 
	    System.out.printf("%d %d  %d %d\n",m_normTestFaceMat.height(),m_normTestFaceMat.width(),m_matEigenFace.height(),m_matEigenFace.width());
	    Mat eigen_test_sample=new Mat();//待识别人脸的投影样本矩阵
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
	    if (min>0&&min<8) {//阈值范围0-10
        	   Mat tureMat=m_matTrainingImage.row(index);//获取在训练样本中对应的图像矩阵1XN
        	   System.out.println(tureMat.dump());
        	   System.out.println(tureMat.rows()+","+tureMat.cols());
        	   //转换为图像大小img_w*img_h的矩阵
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
        	   System.out.println("最相似的图像:"+str);
            	  
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
     	   	
    		System.out.println("库中没有此人！");
    		return null;
	    }
	}
	
	/**将矩阵matImg中的值以图像的方式输出*/
	public WritableImage GetImageFromMatrix(Mat matImg) {
		if (matImg != null) {
			WritableImage wImage = new WritableImage(matImg.width(), matImg.height());
			//得到像素写入器
			PixelWriter pixelWriter = wImage.getPixelWriter();
			if (matImg.channels() == 1) {  //单通道图像
				float[] gray = new float[matImg.height()*matImg.width()];
				matImg.get(0, 0, gray);
				//遍历图像矩阵每个像素，将其写入到目标图像
				for (int y=0; y<matImg.height(); y++) {
					for (int x=0; x<matImg.width(); x++) {
						int pixelIndex = y*matImg.width()+x;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}	
			}
			else if (matImg.channels() == 3) {	//3通道图像
				//遍历源图像每个像素，将其写入到目标图像
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
	
	/**将图像image存放在数据域m_matImage矩阵中*/
	public void GetGrayImgMatFromImage(Image image) {
		if (image != null) {
			PixelReader pixelReader = image.getPixelReader();
			m_matImage = new Mat((int)(image.getHeight()),(int)(image.getWidth()), CvType.CV_32F);
			//遍历原图像每个像素，将其写入到目标图像矩阵
			for (int y=0; y<image.getHeight(); y++) {
				for (int x=0; x<image.getWidth(); x++) {
					Color color = pixelReader.getColor(x, y);
					float gray = (float)((color.getBlue()+color.getGreen()+color.getRed())/3.0);
					m_matImage.put(y, x, gray);
				}
			}
		}
	}
	
	/**从灰度图像集合(训练样本矩阵、特征脸矩阵)矩阵中抽取一行或一列组成二维图像*/
	@SuppressWarnings("null")
	public WritableImage GetImageFromGrayImagesMatrix(Mat matImages, //图像集合矩阵
												  	int imgWidth, //二维图像的宽度
												  	int index,    //取得行或者列的索引
												  	boolean byRow) //true为抽取一行，false为抽取一列
	{
		if (matImages != null || matImages.channels() != 1) {
			int widthMat = matImages.width();
			int heightMat = matImages.height();
			if (byRow == true) { //按行抽取
				int imgHeight = widthMat/imgWidth; //二维图像高度
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				//得到像素写入器
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[widthMat];
				matImages.get(index, 0, gray);
				//遍历图像每个像素
				for (int x=0; x<wImage.getWidth(); x++) {
					for (int y=0; y<wImage.getHeight(); y++) {
						int pixelIndex = x*imgHeight+y;
						Color color = Color.gray(gray[pixelIndex]);
						pixelWriter.setColor(x, y, color);
					}
				}	
				return wImage;
			}
			else { //按列抽取
				int imgHeight = heightMat/imgWidth; //二维图像高度
				WritableImage wImage = new WritableImage(imgWidth, imgHeight);
				//得到像素写入器
				PixelWriter pixelWriter = wImage.getPixelWriter();
				float[] gray = new float[heightMat];
				Mat matTImages = matImages.t(); //矩阵转置
				matTImages.get(index, 0, gray);
				//遍历图像每个像素
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
