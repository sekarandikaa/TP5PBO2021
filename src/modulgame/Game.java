/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modulgame;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
//import static modulgame.dbConnection.con;

/**
 *
 * @author Fauzan
 */
public class Game extends Canvas implements Runnable{
    Window window;
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    
    private int score = 0;
    private int score2 = 0;

    private int time = 10;
    private int time2 = 10;
    
    private int win = 0;
    private int waktu = 0;
    
    private int musuhX = 0;
    private int musuhY = 200;
    private int arahGerak = 0;
    private int kecepatanMusuh = 0;
    
    
    private Thread thread;
    private boolean running = false;
    
    private Handler handler;
    
    public String uname;
    public String sulit;
    public String playerr;

    
    public enum STATE{
        Game,
        GameOver
    };
    
    public STATE gameState = STATE.Game;
    
    public Game(String username, String kesulitan, String player){
        window = new Window(WIDTH, HEIGHT, "Modul praktikum 5", this);
        
        handler = new Handler();
        
        this.addKeyListener(new KeyInput(handler, this));
        sulit = kesulitan;
        uname = username;
        playerr = player;


        if(gameState == STATE.Game){
            //Memainkan BGM Music
            playSound("/8-Bit Sound.wav");
            handler.addObject(new Items(100,150, ID.Item));
            handler.addObject(new Items(200,350, ID.Item));
            handler.addObject(new Player(200,300, ID.Player));
            if(playerr == "2"){//Jika mode 2 player
                handler.addObject(new Player2(600,300, ID.Player2));//Object player 2
            }
        }
        
        //Level kesulitan sesuai pilihan User
        if(sulit == "Easy"){
            kecepatanMusuh = 10;
            time = 20;
        }else if(sulit == "Normal"){
            kecepatanMusuh = 50;
            time = 10;
        }else if(sulit == "Hard"){
            kecepatanMusuh = 100;
            time = 5;
        }
    }

    public synchronized void start(){
        thread = new Thread(this);
        thread.start();
        running = true;
    }
    
    public synchronized void stop(){
        try{
            thread.join();
            running = false;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        
        while(running){
            handler.addObject(new Musuh(musuhX,musuhY, ID.Musuh));//object musuh

            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            while(delta >= 1){
                tick();
                delta--;
            }
            if(running){
                render();
                frames++;
            }
            
            if(System.currentTimeMillis() - timer > 1000){
                timer += 1000;
                //System.out.println("FPS: " + frames);
                frames = 0;
                if(gameState == STATE.Game){
                    if(time>0){
                        time--;
                        waktu++;//Waktu yang digunakan Player hingga Game Over
                        //Pergerakan otomatis musuh
                        if(musuhY <= 550){
                          if((musuhX < 750) && (arahGerak == 0)){
                            musuhX = musuhX + kecepatanMusuh;
                            }else if((musuhX > 0) && (arahGerak == 0)){
                                musuhY = musuhY + 80;
                                arahGerak = 1;
                            }else if((musuhX > 0) && (arahGerak == 1)){
                                musuhX = musuhX - kecepatanMusuh;
                            }else if((musuhX < 750) && (arahGerak == 1)){
                                musuhY = musuhY + 80;
                                arahGerak = 0;
                            }  
                        }else{
                            musuhY = 30;
                        }
                    }else{
                        gameState = STATE.GameOver;
                    }
                }
            }
            //Menghapus object musuh
            GameObject musuhObject = null;

            for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Musuh){
                   musuhObject = handler.object.get(i);
                }
                
            }
            handler.removeObject(musuhObject);
            //Cek apakah player mengenai musuh
            for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Player){
                   if(cekKenaMusuh(handler.object.get(i), musuhObject)){
                        gameState = STATE.GameOver;//game selesai jika mengenai musuh
                        win = 2;
                    }
                }
            }
            if(playerr == "2"){
                for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Player2){
                   if(cekKenaMusuh(handler.object.get(i), musuhObject)){
                        gameState = STATE.GameOver;//game selesai jika mengenai musuh
                        win = 1;
                    }
                }
            }
            }
        }
        stop();
    }
    
    private void tick(){
        handler.tick();
        if(playerr == "1"){//Jika mode 1 player
           if(gameState == STATE.Game){
                GameObject playerObject = null;
                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Player){
                       playerObject = handler.object.get(i);
                    }
                }
                if(playerObject != null){
                    for(int i=0;i< handler.object.size(); i++){
                        if(handler.object.get(i).getId() == ID.Item){
                            if(checkCollision(playerObject, handler.object.get(i))){
                                playSound("/Eat.wav");
                                handler.removeObject(handler.object.get(i));
                                //Menambahkan score dan timer secara random
                                score = score + (int)Math.floor(Math.random()*(20-1+1)+1);
                                time = time + (int)Math.floor(Math.random()*(15-1+1)+1);

                                //Menambahkan object dengan koordinat random, jika item sudah habis
                                handler.addObject(new Items((int)Math.floor(Math.random()*(700-10+1)+1),(int)Math.floor(Math.random()*(500-10+1)+1), ID.Item));
                                break;
                            }
                        }
                    }
                }
            } 
        }else{//Jika mode 2 player
            if(gameState == STATE.Game){
                GameObject playerObject = null;
                GameObject playerObject2 = null;

                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Player){
                       playerObject = handler.object.get(i);
                    }
                }
                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Player2){
                       playerObject2 = handler.object.get(i);
                    }
                }
                if((playerObject != null) && (playerObject2 != null)){
                    for(int i=0;i< handler.object.size(); i++){
                        if(handler.object.get(i).getId() == ID.Item){
                            if(checkCollision(playerObject, handler.object.get(i))){
                                playSound("/Eat.wav");
                                handler.removeObject(handler.object.get(i));
                                //Menambahkan score dan timer secara random
                                score = score + (int)Math.floor(Math.random()*(20-1+1)+1);
                                time = time + (int)Math.floor(Math.random()*(15-1+1)+1);

                                //Menambahkan object dengan koordinat random, jika item sudah habis
                                handler.addObject(new Items((int)Math.floor(Math.random()*(700-10+1)+1),(int)Math.floor(Math.random()*(500-10+1)+1), ID.Item));
                                break;
                            }else if(checkCollision(playerObject2, handler.object.get(i))){
                                playSound("/Eat.wav");
                                handler.removeObject(handler.object.get(i));
                                //Menambahkan score dan timer secara random
                                score2 = score2 + (int)Math.floor(Math.random()*(20-1+1)+1);
                                time = time + (int)Math.floor(Math.random()*(15-1+1)+1);

                                //Menambahkan object dengan koordinat random, jika item sudah habis
                                handler.addObject(new Items((int)Math.floor(Math.random()*(700-10+1)+1),(int)Math.floor(Math.random()*(500-10+1)+1), ID.Item));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static boolean checkCollision(GameObject player, GameObject item){
        boolean result = false;
        
        int sizePlayer = 50;
        int sizeItem = 20;
        
        int playerLeft = player.x;
        int playerRight = player.x + sizePlayer;
        int playerTop = player.y;
        int playerBottom = player.y + sizePlayer;
        
        int itemLeft = item.x;
        int itemRight = item.x + sizeItem;
        int itemTop = item.y;
        int itemBottom = item.y + sizeItem;
        
        if((playerRight > itemLeft ) &&
        (playerLeft < itemRight) &&
        (itemBottom > playerTop) &&
        (itemTop < playerBottom)
        ){
            result = true;
        }
        
        return result;
    }
    
    public static boolean cekKenaMusuh(GameObject player, GameObject musuh){
        boolean result = false;
        
        int sizePlayer = 50;
        int sizeMusuh = 50;
        
        int playerLeft = player.x;
        int playerRight = player.x + sizePlayer;
        int playerTop = player.y;
        int playerBottom = player.y + sizePlayer;
        
        int musuhLeft = musuh.x;
        int musuhRight = musuh.x + sizeMusuh;
        int musuhTop = musuh.y;
        int musuhBottom = musuh.y + sizeMusuh;
        
        if((musuhRight > playerLeft ) &&
        (musuhLeft < playerRight) &&
        (playerBottom > musuhTop) &&
        (playerTop < musuhBottom)
        ){
            result = true;
        }

        return result;
    }
    
    private void render(){
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            this.createBufferStrategy(3);
            return;
        }
        
        Graphics g = bs.getDrawGraphics();
        
        g.setColor(Color.decode("#F1f3f3"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
                
        
        
        if(gameState ==  STATE.Game){
            handler.render(g);
            
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 1.4F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("Score: " +Integer.toString(score), 20, 20);
            
            if(playerr == "2"){
                g.setColor(Color.BLACK);
                g.drawString("Score P2: " +Integer.toString(score2), 120, 20);
            }

            g.setColor(Color.BLACK);
            g.drawString("Time: " +Integer.toString(time), WIDTH-120, 20);
            
        }else{
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 3F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("GAME OVER", WIDTH/2 - 120, HEIGHT/2 - 30);
            
            currentFont = g.getFont();
            Font newScoreFont = currentFont.deriveFont(currentFont.getSize() * 0.5F);
            g.setFont(newScoreFont);
            
            if(playerr == "2"){
                int hasilScore = score + score2;
                
                g.setColor(Color.BLACK);
                g.drawString("Score: " +Integer.toString(hasilScore+waktu), WIDTH/2 - 50, HEIGHT/2 - 10);
            }else{
                g.setColor(Color.BLACK);
                g.drawString("Score: " +Integer.toString(score+waktu), WIDTH/2 - 50, HEIGHT/2 - 10);
            }
            
            g.setColor(Color.BLACK);
            g.drawString("Press Space to Continue", WIDTH/2 - 100, HEIGHT/2 + 60);
        }
        
        g.dispose();
        bs.show();
    }
    
    public static int clamp(int var, int min, int max){
        if(var >= max){
            return var = max;
        }else if(var <= min){
            return var = min;
        }else{
            return var;
        }
    }
    
    public void close(){
        window.CloseWindow();
    }
    
    public void playSound(String filename){
        try {
            // Open an audio input stream.
            URL url = this.getClass().getResource(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            // Get a sound clip resource.
            Clip clip = AudioSystem.getClip();
            // Open audio clip and load samples from the audio input stream.
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException e) {
           e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        } catch (LineUnavailableException e) {
           e.printStackTrace();
        }
    
    }
    
    public void addToTabel(){
        try {
            int hasilScore;
            
            if(playerr == "1"){
                hasilScore = score;
            }else{
                hasilScore = score + score2;
            }
            
            String query = "select * from highscore where Username = '"+uname+"'";
            java.sql.ResultSet rs;
            rs = dbConnection.stm.executeQuery(query);

            if(rs.next()){//Jika username sudah ada, melakukan update
                String user = rs.getString("Username");
                int lastScore = rs.getInt("Score");
                
                if(lastScore < hasilScore+waktu){//Membandingkan besaran score yang didapat dengan score sebelumnya
                    String updateQuery = "update highscore set Score='"+Integer.toString(hasilScore+waktu)+"' where Username='"+uname+"'";
                    int q = dbConnection.stm.executeUpdate(updateQuery); 
                }
            }else{//Jika username baru, melakukan insert
                String sql = "insert into highscore (Username, Score) values ('"+uname+"', '"+Integer.toString(hasilScore+waktu)+"')";//Score yang diinput score game + waktu
                java.sql.PreparedStatement pst;
                pst = dbConnection.con.prepareStatement(sql);
                pst.execute();
            }
        } catch (Exception e) {
            System.err.println("gagal" +e.getMessage());
        }
    }
}
