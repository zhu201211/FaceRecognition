package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

public class response implements Initializable {

	MyModel theModel;
	String[] TestPerson={"剑锋","皮特","小彬","果冻","小杰","小辉","小杨","小峰","小鑫","小林"};
	//String[] TestPerson={"s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11","s12","s13","s14","s15","s16","s17","s18","s19","s20","s21","s22","s23","s24","s25","s26","s27","s28","s29","s30","s31","s32","s33","s34","s35","s36","s37","s38","s39","s40"};
	enum OperationType {NONE, SAMPLEIMAGE, TRAINING, TRAINED, RECOGNITION}; //操作类型
	OperationType currentOperation;
	boolean m_bMark = false; //是否开始标记眼睛
	int m_nIndexofTrainingSamples; //序列图像中的图像索引
	int m_nWidthofTrainingSample; //序列图像中每个图像的宽度
	int m_numMouseClick = 0; //记录标记眼睛时鼠标点击次数
	int lastPoint_x = 0;//保存鼠标选择第一个眼睛位置的x坐标
	int lastPoint_y = 0;//保存鼠标选择第一个眼睛位置的y坐标
	String[] m_strNormedImgFilePath = null; //存放训练样本集各图像路径
	
	@FXML
	private AnchorPane layoutPane;
	@FXML
	private Pane pane_1;
	@FXML
	private ImageView ShowImage_1;
	@FXML
	private ImageView ShowImage_2;
	@FXML
	private TextArea TipText;
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		currentOperation = OperationType.NONE;
		theModel = new MyModel();
	}
	
	@FXML
	private void OnMenuOpenImage(ActionEvent event) {
    	//打开文件对话框
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Resource File");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	theModel.m_matImage=null;
    	theModel.m_matNormImage=null;
    	if (file.exists()) {
    		currentOperation = OperationType.SAMPLEIMAGE;
    		pane_1.getChildren().clear();//清除所有节点，包括圆、ImageView控件
    		pane_1.getChildren().add(ShowImage_1);//将ImageView控件重新添加进来
    		ShowImage_2.setImage(null); //清空结果框图像
    		OpenImageFile(file); //打开文件
    		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //显示灰度图像
    	}	
	}
	
	@FXML
	/**对图像进行归一化：几何归一化和灰度归一化 */
	private void OnMenuGuiyihua(ActionEvent event) {
		if (currentOperation == OperationType.SAMPLEIMAGE 
				&& theModel.m_matImage != null)
			m_bMark = true;
	}
	
	@FXML
	/**保存归一化的图像*/
	private void OnMenuSaveImage(ActionEvent event) {
		if (currentOperation == OperationType.SAMPLEIMAGE && theModel.m_matNormImage != null) {
			//弹出保存文件对话框，找到归一化后的图像保存位置
	    	FileChooser fileChooser = new FileChooser();
	    	fileChooser.setTitle("Save Normalized Image");
	    	File file = fileChooser.showSaveDialog(layoutPane.getScene().getWindow());
	    	if (file != null) {
	    		WritableImage wImage = theModel.GetImageFromMatrix(theModel.m_matNormImage);
	    		
	    		BufferedImage bufferImage = SwingFXUtils.fromFXImage(wImage, null);
	    		try {
	    			ImageIO.write(bufferImage, "png", file);
	    			System.out.println("图片大小："+bufferImage.getWidth()+","+bufferImage.getHeight());
	    			TipText.setText("文件保存成功");
	    			System.out.println("文件保存成功");
	    		} catch (IOException e){
	    			throw new RuntimeException(e);
	    		}
	    	}
		}
		else {
		    	TipText.setText("非采样环境或者无归一化图像，无法进行保存操作");
			System.out.println("非采样环境或者无归一化图像，无法进行保存操作");
		}
	}
	

	@FXML
	/**读入归一化后的训练图像集，很多图像连续读入*/
	private void ReadImage(ActionEvent event) {
	   	//打开文件对话框，找到训练图像集的位置
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Training Image Group");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	if (file.exists()) {
    		currentOperation = OperationType.TRAINING;
    		pane_1.getChildren().clear();//清除所有节点，包括圆、ImageView控件
    		pane_1.getChildren().add(ShowImage_1);//将ImageView控件重新添加进来
    		theModel.ReadTrainingSampleFiles(file);    		
    		//在图像框中显示载入的训练集图像
    		m_nIndexofTrainingSamples = 0;
    		ReadSerialImagesPath(file); //保存图像集中各图像的路径
    		Image iImage = new Image(m_strNormedImgFilePath[m_nIndexofTrainingSamples]);
    		m_nWidthofTrainingSample = (int)iImage.getWidth();
    		ShowImage_1.setImage(iImage); //左边显示训练集中第1张灰度图像
    		WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matTrainingImage,
    				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, true);
    		ShowImage_2.setImage(wImage); //右边显示训练集矩阵中提取的第1张灰度图像
    		TipText.setText("读入图像集完成");
    	}
	}
	
	@FXML
	/**样本集训练，抽取样本特征*/
	private void OnMenuTrainingImage(ActionEvent event) {
		if (currentOperation == OperationType.TRAINING 
				&& theModel.m_matTrainingImage != null) {
			//计算特征脸
			theModel.CalculateEigenFaceMat();
			//输出平均矩阵到项目当前文件夹
			theModel.SavemeanFaceMat();
			//输出训练样本矩阵到项目当前文件夹
			theModel.Savem_matTrainingImage();
			//输出特征脸矩阵到项目当前文件夹
			theModel.SaveEigenFaceMat();
			//输出训练样本集在特征脸空间的投影矩阵到项目当前文件夹
			theModel.SaveEigenTrainingSamplesMat();
			//将阶段标记为训练完毕阶段
			currentOperation = OperationType.TRAINED;
			TipText.setText("训练完成");
			//将计算的特征脸图像显示在右边的图像结果框中
//			m_nIndexofTrainingSamples = 0;
//			WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matEigenFace,
//    				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, false);
//			ShowImage_2.setImage(wImage); //右边显示特征脸矩阵中提取的第1张灰度图像
		}
	}
	
	@FXML
	/**打开一张待识别的图像*/
	private void onMenuOpenTestImage(ActionEvent event) {
    	//打开文件对话框
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Resource File");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	theModel.m_matImage=null;
    	theModel.m_matNormImage=null;
    	if (file.exists()) {
    		currentOperation = OperationType.RECOGNITION;
    		pane_1.getChildren().clear();//清除所有节点，包括圆、ImageView控件
    		pane_1.getChildren().add(ShowImage_1);//将ImageView控件重新添加进来
    		ShowImage_2.setImage(null); //清空结果框图像
    		TipText.setText(null);
    		OpenImageFile(file); //打开文件
    		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //显示灰度图像
    	}	
	}
	
	@FXML
	/**对待识别的图像进行归一化，和采集阶段类似*/
	private void onMenuNormforRecogntion(ActionEvent event) {
		if (currentOperation == OperationType.RECOGNITION 
				&& theModel.m_matImage != null)
			m_bMark = true;
	}
	
	@FXML
	/**对归一化后的待识别图像进行识别*/
	private void onMenuRecognizeImage(ActionEvent event) {
		//判断特征脸矩阵文件存在否，待识别图像是否归一化，如果两个都满足则进行识别
		if (currentOperation == OperationType.RECOGNITION ) { //待识别人脸已归一化
			if (theModel.m_matEigenFace == null //特征脸矩阵为空
				|| theModel.m_matEigenTrainingSamples==null) { //特征脸训练样本集投影矩阵为空
				//读入特征脸矩阵和特征脸训练样本集投影矩阵
				try {
				    	theModel.ReadmeanFaceMatFromFile();
				    	theModel.Readm_matTrainingImageFromFile();
					theModel.ReadEigenFaceMatFromFile();
					theModel.ReadEigenTrainingSamplesMatFromFile();
					Mat temp=theModel.GetSimilarFaceMat();
					if(temp!=null){
        					WritableImage wImage = theModel.GetImageFromMatrix(temp);
        					//将结果图像显示在结果图像框中
        					TipText.setText("识别结果:"+TestPerson[theModel.person-1]);
        					System.out.println("识别结果:"+TestPerson[theModel.person-1]);
        					ShowImage_2.setImage(wImage);
					}
					else{
					    	TipText.setText("库中没有此人!");
					}
				} catch (IOException e) {
					System.out.println(e.toString());
				}
			}
			else { //特征脸矩阵不为空
			    	Mat temp=theModel.GetSimilarFaceMat();
				if(temp!=null){
					WritableImage wImage = theModel.GetImageFromMatrix(temp);
					//将结果图像显示在结果图像框中
					TipText.setText("识别结果:"+TestPerson[theModel.person-1]);
					System.out.println("识别结果:"+TestPerson[theModel.person-1]);
					ShowImage_2.setImage(wImage);
				}
				else{
				    	TipText.setText("库中没有此人!");
				}
			}
		}
	}
	
	@FXML
	private void OnMenuExit(ActionEvent event) {
		Platform.exit();
	}
	
	@FXML
	/**通过鼠标选择双眼*/
	private void onMouseClicked(MouseEvent event) {
		if ((currentOperation == OperationType.SAMPLEIMAGE 
				|| currentOperation == OperationType.RECOGNITION)
				&& m_bMark == true && theModel.m_matImage != null)
		{
			Circle c = new Circle(event.getX(), event.getY(), 3);
			c.setFill(Color.RED);
			pane_1.getChildren().add(c);
			System.out.println("x="+event.getX()+" "+"y="+event.getY());
			if (m_numMouseClick == 0) {
				m_numMouseClick++;
				lastPoint_x = (int)event.getX();
				lastPoint_y = (int)event.getY();
			}
			else {
				theModel.NormalizeImage(lastPoint_x, lastPoint_y, 
						(int)event.getX(), (int)event.getY());
				m_numMouseClick = 0;
				m_bMark = false;
				
				//将归一化结果显示在右边的ivShowReultImage中
				if (theModel.m_matNormImage != null) {
					WritableImage wImage = theModel.GetImageFromMatrix(theModel.m_matNormImage);
					//将结果图像显示在结果图像框中
					ShowImage_2.setImage(wImage);
				}
			}
		}
		else if (currentOperation == OperationType.TRAINING 
					&& theModel.m_matTrainingImage != null
					&& m_bMark == false) 
		{
			if (event.getButton() == MouseButton.PRIMARY) {//鼠标左键点击向后翻图像
	    		m_nIndexofTrainingSamples++;
	    		if (m_nIndexofTrainingSamples>=m_strNormedImgFilePath.length)
	    			m_nIndexofTrainingSamples=m_strNormedImgFilePath.length-1;
			}else if (event.getButton() == MouseButton.SECONDARY) {//鼠标右键点击向前翻图像
				m_nIndexofTrainingSamples--;
				if (m_nIndexofTrainingSamples<0) m_nIndexofTrainingSamples=0;
			}
			//在图像框中显示载入的训练集图像
        		Image iImage = new Image(m_strNormedImgFilePath[m_nIndexofTrainingSamples]);
        		ShowImage_1.setImage(iImage); //左边显示训练集中第1张灰度图像
        		WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matTrainingImage,
        				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, true);
        		ShowImage_2.setImage(wImage); //右边显示训练集矩阵中提取的第1张灰度图像
		}
		else if (currentOperation == OperationType.TRAINED 
					&& theModel.m_matEigenFace != null
					&& m_bMark == false)
		{
			if (event.getButton() == MouseButton.PRIMARY) {//鼠标左键点击向后翻图像
	    		m_nIndexofTrainingSamples++;
	    		if (m_nIndexofTrainingSamples>=theModel.m_matEigenFace.width())
	    			m_nIndexofTrainingSamples=theModel.m_matEigenFace.width()-1;
			}else if (event.getButton() == MouseButton.SECONDARY) {//鼠标右键点击向前翻图像
				m_nIndexofTrainingSamples--;
				if (m_nIndexofTrainingSamples<0) m_nIndexofTrainingSamples=0;
			}
			//在图像结果框中显示特征脸图像
			WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matEigenFace,m_nWidthofTrainingSample, m_nIndexofTrainingSamples, false);
    			ShowImage_2.setImage(wImage); //右边显示训练集矩阵中提取的第1张灰度图像			
		}
	}
	
	/**打开一个图像文件，得到它的灰度矩阵*/
	public void OpenImageFile(File file) {
		//进行文件读入并显示
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); //使用本地文件的URL路径
		Image imgSourceImage = new Image(filePath);
		theModel.GetGrayImgMatFromImage(imgSourceImage);
		//调整输入图像矩阵大小300*300
		Mat m=new Mat();
		int width;
		int height;
		if(theModel.m_matImage.width()>=theModel.m_matImage.height())
		{
			width=300;
			height=(int)(300.0/theModel.m_matImage.width()*theModel.m_matImage.height());
		}else
		{
			height=300;
			width=(int)(300.0/theModel.m_matImage.height()*theModel.m_matImage.width());
		}
		System.out.printf("%d %d\n",width,height);
		Imgproc.resize(theModel.m_matImage,m,new Size(width,height)); 
		theModel.m_matImage=m;
	}

	/**读入序列图像文件路径，序列图像位于file文件夹中*/
	public void ReadSerialImagesPath(File file) {
		String fileParent = "file:///" + file.getParent();//获得训练集所在目录
		fileParent = fileParent.replace('\\', '/'); //使用本地文件的URL路径
		m_strNormedImgFilePath = new String[theModel.numPerson*theModel.numSamplePerPerson];
		int n = 0;
		for (int i=1; i<=theModel.numPerson; i++) {//第i个人
			for (int j=1; j<=theModel.numSamplePerPerson; j++) //第j个样本
			{	
				String s = fileParent+"/s"+i+"_"+j+".bmp"; //图像si_j.bmp路径
				m_strNormedImgFilePath[n] = new String(s);
				n++;
			}
		}
	}
	
	@FXML
	public void onMenuOpenCamera(){//摄像头
	    Camera c= new Camera();
	    while(c.bt==false)
	    {
		System.out.println(" ");
	    }
	    File file=new File("F:\\test.bmp");
	    theModel.m_matImage=null;
	    theModel.m_matNormImage=null;
	    if (file.exists()) {
		currentOperation = OperationType.RECOGNITION;
		pane_1.getChildren().clear();//清除所有节点，包括圆、ImageView控件
		pane_1.getChildren().add(ShowImage_1);//将ImageView控件重新添加进来
		ShowImage_2.setImage(null); //清空结果框图像
		OpenImageFile(file); //打开文件
		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //显示灰度图像	
	    }
	}
	
	@FXML
	public void onMenuOpenCamera_1(){//摄像头
	    Camera c= new Camera();
	    while(c.bt==false)
	    {
		System.out.println(" ");
	    }
	    File file=new File("F:\\test.bmp");
	    theModel.m_matImage=null;
	    theModel.m_matNormImage=null;
	    if (file.exists()) {
		currentOperation = OperationType.SAMPLEIMAGE;
		pane_1.getChildren().clear();//清除所有节点，包括圆、ImageView控件
		pane_1.getChildren().add(ShowImage_1);//将ImageView控件重新添加进来
		ShowImage_2.setImage(null); //清空结果框图像
		OpenImageFile(file); //打开文件
		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //显示灰度图像	
	    }
	}
}
