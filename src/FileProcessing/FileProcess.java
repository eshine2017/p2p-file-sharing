package FileProcessing;

import java.io.*;

public class FileProcess {
    private String fileName;
    private String inFilePath;
    private String outFilePath;
    private int fileSize;
    private int pieceSize;
    //private int fileNumber = 0;
    private int numOfPiece = fileSize/pieceSize +1;
    private int lastSize = fileSize-(numOfPiece-1)*pieceSize;

    public FileProcess(String fileName, String inFilePath, String outFilePath, int fileSize, int pieceSize){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.inFilePath = inFilePath;
        this.outFilePath = outFilePath;
    }

    //protect variable and access to get
    public int getFileSize(){

        return this.fileSize;
    }

    public int getPieceSize(){

        return this.pieceSize;
    }

    public int getNumOfPiece(){

        return this.numOfPiece;
    }

    public int getLastSize(){

        return this.lastSize;
    }

    //divide file to some subfile
    public void divide(){
        //check input error
        if(inFilePath==null){
            System.out.println("please check input path and try again");
            return;
        }
        if(outFilePath==null){
            System.out.println("please ckeck output path and try again");
            return;
        }
        if(fileSize==0){
            System.out.println("please ckeck the input file");
            return;
        }
        if(pieceSize==0){
            System.out.println("piece size illegal");
            return;
        }
        try {
            //read file into stream
            FileInputStream fileIn = new FileInputStream(inFilePath + fileName);
            //use buffer to store
            byte[] filebuf = new byte[fileSize];
            //read file to buffer
            int m = fileIn.read(filebuf);
            if(m!=fileSize){
                System.out.println("read error");
                return;
            }
            fileIn.close();
            //divide pieces with full size
            for (int i = 1; i < numOfPiece; i++) {
                System.out.println("trying to creat part." + i + " with size of " + pieceSize + " bytes");
                FileOutputStream fileOut = new FileOutputStream(outFilePath + fileName + i + ".part");
                //output subfile
                fileOut.write(filebuf, pieceSize * (i - 1), pieceSize);
                fileOut.close();
                System.out.println("part." + i + " of file created successfully");
            }
            
            //divide final piece
            System.out.println("trying to creat last part" + numOfPiece + " with size of " + lastSize + "bytes");
            FileOutputStream fileOut = new FileOutputStream(outFilePath + fileName + numOfPiece + ".part");
            fileOut.write(filebuf, pieceSize * (numOfPiece - 1), lastSize);
            fileOut.close();
            System.out.println("last part of file created successfully");
            System.out.println("Divide successfully");
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void combine(String[] partsPath, String outFilePath){
        if(partsPath==null||partsPath.length==0||outFilePath==null){
            System.out.println("Import error, please check and try again");
            return;
        }
        //File[] files = new File[numOfPiece];
        /*for(int i=0; i<numOfPiece; i++){
            files[i] = new File(partsPath[i]+fileName);
        }*/
        try {
            BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(outFilePath + fileName));
            System.out.println("trying to recontribute flie " + fileName +" at " +outFilePath);
            byte[] buf = new byte[pieceSize];
            try {
                for (int i = 0; i < numOfPiece; i++) {
                    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(partsPath[i] + fileName + i + ".part"));
                    System.out.println("trying to recontribute part " + i +" of the file");
                    int count;
                    while ((count = inStream.read(buf)) != 0) {
                        outStream.write(buf, 0, count);
                        System.out.println("part" + i + " recontribute successfully");
                    }
                    inStream.close();
                }
                outStream.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("file recontribute successfully");
    }
}
