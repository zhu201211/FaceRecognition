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
	String[] TestPerson={"����","Ƥ��","С��","����","С��","С��","С��","С��","С��","С��"};
	//String[] TestPerson={"s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11","s12","s13","s14","s15","s16","s17","s18","s19","s20","s21","s22","s23","s24","s25","s26","s27","s28","s29","s30","s31","s32","s33","s34","s35","s36","s37","s38","s39","s40"};
	enum OperationType {NONE, SAMPLEIMAGE, TRAINING, TRAINED, RECOGNITION}; //��������
	OperationType currentOperation;
	boolean m_bMark = false; //�Ƿ�ʼ����۾�
	int m_nIndexofTrainingSamples; //����ͼ���е�ͼ������
	int m_nWidthofTrainingSample; //����ͼ����ÿ��ͼ��Ŀ��
	int m_numMouseClick = 0; //��¼����۾�ʱ���������
	int lastPoint_x = 0;//�������ѡ���һ���۾�λ�õ�x����
	int lastPoint_y = 0;//�������ѡ���һ���۾�λ�õ�y����
	String[] m_strNormedImgFilePath = null; //���ѵ����������ͼ��·��
	
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
    	//���ļ��Ի���
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Resource File");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	theModel.m_matImage=null;
    	theModel.m_matNormImage=null;
    	if (file.exists()) {
    		currentOperation = OperationType.SAMPLEIMAGE;
    		pane_1.getChildren().clear();//������нڵ㣬����Բ��ImageView�ؼ�
    		pane_1.getChildren().add(ShowImage_1);//��ImageView�ؼ�������ӽ���
    		ShowImage_2.setImage(null); //��ս����ͼ��
    		OpenImageFile(file); //���ļ�
    		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //��ʾ�Ҷ�ͼ��
    	}	
	}
	
	@FXML
	/**��ͼ����й�һ�������ι�һ���ͻҶȹ�һ�� */
	private void OnMenuGuiyihua(ActionEvent event) {
		if (currentOperation == OperationType.SAMPLEIMAGE 
				&& theModel.m_matImage != null)
			m_bMark = true;
	}
	
	@FXML
	/**�����һ����ͼ��*/
	private void OnMenuSaveImage(ActionEvent event) {
		if (currentOperation == OperationType.SAMPLEIMAGE && theModel.m_matNormImage != null) {
			//���������ļ��Ի����ҵ���һ�����ͼ�񱣴�λ��
	    	FileChooser fileChooser = new FileChooser();
	    	fileChooser.setTitle("Save Normalized Image");
	    	File file = fileChooser.showSaveDialog(layoutPane.getScene().getWindow());
	    	if (file != null) {
	    		WritableImage wImage = theModel.GetImageFromMatrix(theModel.m_matNormImage);
	    		
	    		BufferedImage bufferImage = SwingFXUtils.fromFXImage(wImage, null);
	    		try {
	    			ImageIO.write(bufferImage, "png", file);
	    			System.out.println("ͼƬ��С��"+bufferImage.getWidth()+","+bufferImage.getHeight());
	    			TipText.setText("�ļ�����ɹ�");
	    			System.out.println("�ļ�����ɹ�");
	    		} catch (IOException e){
	    			throw new RuntimeException(e);
	    		}
	    	}
		}
		else {
		    	TipText.setText("�ǲ������������޹�һ��ͼ���޷����б������");
			System.out.println("�ǲ������������޹�һ��ͼ���޷����б������");
		}
	}
	

	@FXML
	/**�����һ�����ѵ��ͼ�񼯣��ܶ�ͼ����������*/
	private void ReadImage(ActionEvent event) {
	   	//���ļ��Ի����ҵ�ѵ��ͼ�񼯵�λ��
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Training Image Group");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	if (file.exists()) {
    		currentOperation = OperationType.TRAINING;
    		pane_1.getChildren().clear();//������нڵ㣬����Բ��ImageView�ؼ�
    		pane_1.getChildren().add(ShowImage_1);//��ImageView�ؼ�������ӽ���
    		theModel.ReadTrainingSampleFiles(file);    		
    		//��ͼ�������ʾ�����ѵ����ͼ��
    		m_nIndexofTrainingSamples = 0;
    		ReadSerialImagesPath(file); //����ͼ���и�ͼ���·��
    		Image iImage = new Image(m_strNormedImgFilePath[m_nIndexofTrainingSamples]);
    		m_nWidthofTrainingSample = (int)iImage.getWidth();
    		ShowImage_1.setImage(iImage); //�����ʾѵ�����е�1�ŻҶ�ͼ��
    		WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matTrainingImage,
    				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, true);
    		ShowImage_2.setImage(wImage); //�ұ���ʾѵ������������ȡ�ĵ�1�ŻҶ�ͼ��
    		TipText.setText("����ͼ�����");
    	}
	}
	
	@FXML
	/**������ѵ������ȡ��������*/
	private void OnMenuTrainingImage(ActionEvent event) {
		if (currentOperation == OperationType.TRAINING 
				&& theModel.m_matTrainingImage != null) {
			//����������
			theModel.CalculateEigenFaceMat();
			//���ƽ��������Ŀ��ǰ�ļ���
			theModel.SavemeanFaceMat();
			//���ѵ������������Ŀ��ǰ�ļ���
			theModel.Savem_matTrainingImage();
			//���������������Ŀ��ǰ�ļ���
			theModel.SaveEigenFaceMat();
			//���ѵ�����������������ռ��ͶӰ������Ŀ��ǰ�ļ���
			theModel.SaveEigenTrainingSamplesMat();
			//���׶α��Ϊѵ����Ͻ׶�
			currentOperation = OperationType.TRAINED;
			TipText.setText("ѵ�����");
			//�������������ͼ����ʾ���ұߵ�ͼ��������
//			m_nIndexofTrainingSamples = 0;
//			WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matEigenFace,
//    				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, false);
//			ShowImage_2.setImage(wImage); //�ұ���ʾ��������������ȡ�ĵ�1�ŻҶ�ͼ��
		}
	}
	
	@FXML
	/**��һ�Ŵ�ʶ���ͼ��*/
	private void onMenuOpenTestImage(ActionEvent event) {
    	//���ļ��Ի���
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Open Resource File");
    	File file = fileChooser.showOpenDialog(layoutPane.getScene().getWindow());
    	theModel.m_matImage=null;
    	theModel.m_matNormImage=null;
    	if (file.exists()) {
    		currentOperation = OperationType.RECOGNITION;
    		pane_1.getChildren().clear();//������нڵ㣬����Բ��ImageView�ؼ�
    		pane_1.getChildren().add(ShowImage_1);//��ImageView�ؼ�������ӽ���
    		ShowImage_2.setImage(null); //��ս����ͼ��
    		TipText.setText(null);
    		OpenImageFile(file); //���ļ�
    		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //��ʾ�Ҷ�ͼ��
    	}	
	}
	
	@FXML
	/**�Դ�ʶ���ͼ����й�һ�����Ͳɼ��׶�����*/
	private void onMenuNormforRecogntion(ActionEvent event) {
		if (currentOperation == OperationType.RECOGNITION 
				&& theModel.m_matImage != null)
			m_bMark = true;
	}
	
	@FXML
	/**�Թ�һ����Ĵ�ʶ��ͼ�����ʶ��*/
	private void onMenuRecognizeImage(ActionEvent event) {
		//�ж������������ļ����ڷ񣬴�ʶ��ͼ���Ƿ��һ����������������������ʶ��
		if (currentOperation == OperationType.RECOGNITION ) { //��ʶ�������ѹ�һ��
			if (theModel.m_matEigenFace == null //����������Ϊ��
				|| theModel.m_matEigenTrainingSamples==null) { //������ѵ��������ͶӰ����Ϊ��
				//���������������������ѵ��������ͶӰ����
				try {
				    	theModel.ReadmeanFaceMatFromFile();
				    	theModel.Readm_matTrainingImageFromFile();
					theModel.ReadEigenFaceMatFromFile();
					theModel.ReadEigenTrainingSamplesMatFromFile();
					Mat temp=theModel.GetSimilarFaceMat();
					if(temp!=null){
        					WritableImage wImage = theModel.GetImageFromMatrix(temp);
        					//�����ͼ����ʾ�ڽ��ͼ�����
        					TipText.setText("ʶ����:"+TestPerson[theModel.person-1]);
        					System.out.println("ʶ����:"+TestPerson[theModel.person-1]);
        					ShowImage_2.setImage(wImage);
					}
					else{
					    	TipText.setText("����û�д���!");
					}
				} catch (IOException e) {
					System.out.println(e.toString());
				}
			}
			else { //����������Ϊ��
			    	Mat temp=theModel.GetSimilarFaceMat();
				if(temp!=null){
					WritableImage wImage = theModel.GetImageFromMatrix(temp);
					//�����ͼ����ʾ�ڽ��ͼ�����
					TipText.setText("ʶ����:"+TestPerson[theModel.person-1]);
					System.out.println("ʶ����:"+TestPerson[theModel.person-1]);
					ShowImage_2.setImage(wImage);
				}
				else{
				    	TipText.setText("����û�д���!");
				}
			}
		}
	}
	
	@FXML
	private void OnMenuExit(ActionEvent event) {
		Platform.exit();
	}
	
	@FXML
	/**ͨ�����ѡ��˫��*/
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
				
				//����һ�������ʾ���ұߵ�ivShowReultImage��
				if (theModel.m_matNormImage != null) {
					WritableImage wImage = theModel.GetImageFromMatrix(theModel.m_matNormImage);
					//�����ͼ����ʾ�ڽ��ͼ�����
					ShowImage_2.setImage(wImage);
				}
			}
		}
		else if (currentOperation == OperationType.TRAINING 
					&& theModel.m_matTrainingImage != null
					&& m_bMark == false) 
		{
			if (event.getButton() == MouseButton.PRIMARY) {//������������ͼ��
	    		m_nIndexofTrainingSamples++;
	    		if (m_nIndexofTrainingSamples>=m_strNormedImgFilePath.length)
	    			m_nIndexofTrainingSamples=m_strNormedImgFilePath.length-1;
			}else if (event.getButton() == MouseButton.SECONDARY) {//����Ҽ������ǰ��ͼ��
				m_nIndexofTrainingSamples--;
				if (m_nIndexofTrainingSamples<0) m_nIndexofTrainingSamples=0;
			}
			//��ͼ�������ʾ�����ѵ����ͼ��
        		Image iImage = new Image(m_strNormedImgFilePath[m_nIndexofTrainingSamples]);
        		ShowImage_1.setImage(iImage); //�����ʾѵ�����е�1�ŻҶ�ͼ��
        		WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matTrainingImage,
        				m_nWidthofTrainingSample, m_nIndexofTrainingSamples, true);
        		ShowImage_2.setImage(wImage); //�ұ���ʾѵ������������ȡ�ĵ�1�ŻҶ�ͼ��
		}
		else if (currentOperation == OperationType.TRAINED 
					&& theModel.m_matEigenFace != null
					&& m_bMark == false)
		{
			if (event.getButton() == MouseButton.PRIMARY) {//������������ͼ��
	    		m_nIndexofTrainingSamples++;
	    		if (m_nIndexofTrainingSamples>=theModel.m_matEigenFace.width())
	    			m_nIndexofTrainingSamples=theModel.m_matEigenFace.width()-1;
			}else if (event.getButton() == MouseButton.SECONDARY) {//����Ҽ������ǰ��ͼ��
				m_nIndexofTrainingSamples--;
				if (m_nIndexofTrainingSamples<0) m_nIndexofTrainingSamples=0;
			}
			//��ͼ����������ʾ������ͼ��
			WritableImage wImage = theModel.GetImageFromGrayImagesMatrix(theModel.m_matEigenFace,m_nWidthofTrainingSample, m_nIndexofTrainingSamples, false);
    			ShowImage_2.setImage(wImage); //�ұ���ʾѵ������������ȡ�ĵ�1�ŻҶ�ͼ��			
		}
	}
	
	/**��һ��ͼ���ļ����õ����ĻҶȾ���*/
	public void OpenImageFile(File file) {
		//�����ļ����벢��ʾ
		String filePath = "file:///" + file.getPath();
		filePath = filePath.replace('\\', '/'); //ʹ�ñ����ļ���URL·��
		Image imgSourceImage = new Image(filePath);
		theModel.GetGrayImgMatFromImage(imgSourceImage);
		//��������ͼ������С300*300
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

	/**��������ͼ���ļ�·��������ͼ��λ��file�ļ�����*/
	public void ReadSerialImagesPath(File file) {
		String fileParent = "file:///" + file.getParent();//���ѵ��������Ŀ¼
		fileParent = fileParent.replace('\\', '/'); //ʹ�ñ����ļ���URL·��
		m_strNormedImgFilePath = new String[theModel.numPerson*theModel.numSamplePerPerson];
		int n = 0;
		for (int i=1; i<=theModel.numPerson; i++) {//��i����
			for (int j=1; j<=theModel.numSamplePerPerson; j++) //��j������
			{	
				String s = fileParent+"/s"+i+"_"+j+".bmp"; //ͼ��si_j.bmp·��
				m_strNormedImgFilePath[n] = new String(s);
				n++;
			}
		}
	}
	
	@FXML
	public void onMenuOpenCamera(){//����ͷ
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
		pane_1.getChildren().clear();//������нڵ㣬����Բ��ImageView�ؼ�
		pane_1.getChildren().add(ShowImage_1);//��ImageView�ؼ�������ӽ���
		ShowImage_2.setImage(null); //��ս����ͼ��
		OpenImageFile(file); //���ļ�
		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //��ʾ�Ҷ�ͼ��	
	    }
	}
	
	@FXML
	public void onMenuOpenCamera_1(){//����ͷ
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
		pane_1.getChildren().clear();//������нڵ㣬����Բ��ImageView�ؼ�
		pane_1.getChildren().add(ShowImage_1);//��ImageView�ؼ�������ӽ���
		ShowImage_2.setImage(null); //��ս����ͼ��
		OpenImageFile(file); //���ļ�
		ShowImage_1.setImage(theModel.GetImageFromMatrix(theModel.m_matImage)); //��ʾ�Ҷ�ͼ��	
	    }
	}
}
