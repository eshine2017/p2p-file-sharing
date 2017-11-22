package FileProcessing;

import java.io.*;

public class FileProcess {
    private String fileName;
    private String inFilePath;
    private String outFilePath;
    private int fileSize;
    private int pieceSize;
    //private int fileNumber = 0;
    private int numOfPiece;
    private int lastSize;

    public FileProcess(String fileName, String inFilePath, String outFilePath, int fileSize, int pieceSize){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.inFilePath = inFilePath;
        this.outFilePath = outFilePath;

        if(fileSize%pieceSize==0) {
            numOfPiece = fileSize/pieceSize;
        }else {
            numOfPiece = fileSize/pieceSize +1;
        }
        lastSize = fileSize-(numOfPiece-1)*pieceSize;
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
            for (int i = 0; i < numOfPiece-1; i++) {
                System.out.println("trying to creat part." + i + " with size of " + pieceSize + " bytes");
                FileOutputStream fileOut = new FileOutputStream(outFilePath + fileName + i + ".part");
                //output subfile
                fileOut.write(filebuf, pieceSize * i, pieceSize);
                fileOut.flush();
                fileOut.close();
                System.out.println("part." + i + " of file created successfully");
            }
            
            //divide final piece
            System.out.println("trying to creat last part" + (numOfPiece-1) + " with size of " + lastSize + "bytes");
            FileOutputStream fileOut = new FileOutputStream(outFilePath + fileName + (numOfPiece-1) + ".part");
            fileOut.write(filebuf, pieceSize * (numOfPiece - 1), lastSize);
            fileOut.flush();
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

    public void combine(String outFilePath){
        if(outFilePath==null){
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
            try {
                //byte[] buf = new byte[pieceSize];
                for (int i = 0; i < numOfPiece; i++) {
                    byte[] buf = new byte[pieceSize];
                    FileInputStream inStream = new FileInputStream(outFilePath + fileName + i + ".part");
                    System.out.println("trying to recontribute part " + i +" of the file");
                    int count;
                    if ((count = inStream.read(buf)) != 0) {
                        outStream.write(buf, 0, count);
                        System.out.println("part" + i + " recontribute successfully");
                    }
                    inStream.close();
                    File file = new File(outFilePath + fileName + i + ".part");
                    file.delete();
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
