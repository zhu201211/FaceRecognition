package application;


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;



public class Camera {
	public static int flag;
	public boolean bt=false;
   public static void main(String[] args) {
	new Camera();
}
	public Camera() {
		Button button = new Button("È·¶¨");
		button.setSize(50, 20);
		Button button1 = new Button("ÍË³ö");
		button1.setSize(50, 20);
		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());

		WebcamPanel panel = new WebcamPanel(webcam);
		panel.setFPSDisplayed(true);
		panel.setDisplayDebugInfo(true);
		panel.setImageSizeDisplayed(true);
		panel.setMirrored(true);

		JFrame window = new JFrame("Test webcam panel");
		JPanel pane = new JPanel();
		pane.add(button);
		pane.add(button1);
		window.add(panel, BorderLayout.CENTER);
		window.add(pane, BorderLayout.SOUTH);
		window.setResizable(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				webcam.open();

				// get image
				BufferedImage image = webcam.getImage();

				// save image to PNG file
				try {
					ImageIO.write(image, "PNG", new File("F:/test.bmp"));
					bt=true;
					flag=1;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		button1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				webcam.close();
				window.dispose();
				bt=true;
			}
		});
	}
}
