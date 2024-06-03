package com.snakegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SnakeGame extends JFrame implements ActionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int DOT_SIZE = 40;
    private static final int RAND_POS_X = WIDTH / DOT_SIZE;
    private static final int RAND_POS_Y = HEIGHT / DOT_SIZE;
    private static final int DELAY = 140;

    private final List<Point> snake = new CopyOnWriteArrayList<>();
    private Point apple;
    private boolean leftDirection = false;
    private boolean rightDirection = true;
    private boolean upDirection = false;
    private boolean downDirection = false;
    private boolean inGame = true;
    private Timer timer;

    private int score = 0;
    private String playerName;

    private PrintWriter out;
    private BufferedReader in;

    // Images
    private Image appleImage;
    private Image headUp, headDown, headLeft, headRight;
    private Image bodyHorizontal, bodyVertical;
    private Image bodyTopLeft, bodyTopRight, bodyBottomLeft, bodyBottomRight;
    private Image tailUp, tailDown, tailLeft, tailRight;
    private Image backgroundImage;

    public SnakeGame(String playerName) {
        this.playerName = playerName;
        loadImages();
        initBoard();
        initNetwork();
    }

    private void loadImages() {
        try {
            appleImage = new ImageIcon(getClass().getClassLoader().getResource("resources/apple.png")).getImage();

            headUp = new ImageIcon(getClass().getClassLoader().getResource("resources/head_up.png")).getImage();
            headDown = new ImageIcon(getClass().getClassLoader().getResource("resources/head_down.png")).getImage();
            headLeft = new ImageIcon(getClass().getClassLoader().getResource("resources/head_left.png")).getImage();
            headRight = new ImageIcon(getClass().getClassLoader().getResource("resources/head_right.png")).getImage();

            bodyHorizontal = new ImageIcon(getClass().getClassLoader().getResource("resources/body_horizontal.png")).getImage();
            bodyVertical = new ImageIcon(getClass().getClassLoader().getResource("resources/body_vertical.png")).getImage();

            bodyTopLeft = new ImageIcon(getClass().getClassLoader().getResource("resources/body_topleft.png")).getImage();
            bodyTopRight = new ImageIcon(getClass().getClassLoader().getResource("resources/body_topright.png")).getImage();
            bodyBottomLeft = new ImageIcon(getClass().getClassLoader().getResource("resources/body_bottomleft.png")).getImage();
            bodyBottomRight = new ImageIcon(getClass().getClassLoader().getResource("resources/body_bottomright.png")).getImage();

            tailUp = new ImageIcon(getClass().getClassLoader().getResource("resources/tail_up.png")).getImage();
            tailDown = new ImageIcon(getClass().getClassLoader().getResource("resources/tail_down.png")).getImage();
            tailLeft = new ImageIcon(getClass().getClassLoader().getResource("resources/tail_left.png")).getImage();
            tailRight = new ImageIcon(getClass().getClassLoader().getResource("resources/tail_right.png")).getImage();

            backgroundImage = new ImageIcon(getClass().getClassLoader().getResource("resources/background.png")).getImage();

            System.out.println("Images loaded successfully");
        } catch (Exception e) {
            System.out.println("Failed to load images");
            e.printStackTrace();
        }
    }

    private void initBoard() {
        addKeyListener(new TAdapter());
        setBackground(Color.white);
        setFocusable(true);

        setSize(WIDTH, HEIGHT);
        setResizable(false);

        setTitle("Snake Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initGame();
        repaint(); // Force repaint
    }

    private void initGame() {
        snake.clear();
        for (int z = 0; z < 3; z++) {
            snake.add(new Point((50 / DOT_SIZE) * DOT_SIZE - z * DOT_SIZE, (50 / DOT_SIZE) * DOT_SIZE));
        }

        locateApple();

        timer = new Timer(DELAY, this);
        timer.start();
    }

    private void initNetwork() {
        try {
            Socket socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                String response;
                try {
                    while ((response = in.readLine()) != null) {
                        System.out.println("Server: " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void locateApple() {
        int r = (int) (Math.random() * RAND_POS_X);
        int q = (int) (Math.random() * RAND_POS_Y);
        apple = new Point(r * DOT_SIZE, q * DOT_SIZE);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        if (inGame) {
            drawApple(g);
            drawSnake(g);
            drawScore(g);
        } else {
            gameOver(g);  // Переместите вызов gameOver сюда
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawApple(Graphics g) {
        g.drawImage(appleImage, apple.x, apple.y, this);
    }

    private void drawSnake(Graphics g) {
        // Draw head
        Point head = snake.get(0);
        Image headImage = null;
        if (leftDirection) headImage = headLeft;
        else if (rightDirection) headImage = headRight;
        else if (upDirection) headImage = headUp;
        else if (downDirection) headImage = headDown;

        g.drawImage(headImage, head.x, head.y, this);

        // Draw body
        for (int i = 1; i < snake.size() - 1; i++) {
            Point p = snake.get(i);
            Point next = snake.get(i + 1);
            Point prev = snake.get(i - 1);
            Image img = bodyHorizontal; // Default to horizontal body segment

            if (prev.x == next.x) img = bodyVertical;
            else if (prev.y == next.y) img = bodyHorizontal;
            else if (prev.x < p.x && next.y < p.y || next.x < p.x && prev.y < p.y) img = bodyTopLeft;
            else if (prev.x > p.x && next.y < p.y || next.x > p.x && prev.y < p.y) img = bodyTopRight;
            else if (prev.x < p.x && next.y > p.y || next.x < p.x && prev.y > p.y) img = bodyBottomLeft;
            else if (prev.x > p.x && next.y > p.y || next.x > p.x && prev.y > p.y) img = bodyBottomRight;

            g.drawImage(img, p.x, p.y, this);
        }

        // Draw tail
        Point tail = snake.get(snake.size() - 1);
        Point tailPrev = snake.get(snake.size() - 2);
        Image tailImage = null;

        if (tailPrev.x < tail.x) tailImage = tailRight;
        else if (tailPrev.x > tail.x) tailImage = tailLeft;
        else if (tailPrev.y < tail.y) tailImage = tailDown;
        else if (tailPrev.y > tail.y) tailImage = tailUp;

        g.drawImage(tailImage, tail.x, tail.y, this);

    }

    private void drawScore(Graphics g) {
        g.setColor(Color.black);
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        // Отображение счета внизу слева. Учитываем высоту строки, чтобы текст не находился слишком близко к краю окна.
        g.drawString("Score: " + score, 10, HEIGHT - 10);
    }

    private void gameOver(Graphics g) {
        String msg = score >= 20 ? "Congratulations! You scored 20!" : "Game Over";
        String scoreMsg = "Final Score: " + score;
        String replayMsg = "Press R to Restart";
        Font font = new Font("Helvetica", Font.BOLD, 14);

        g.setColor(Color.black);
        g.setFont(font);
        FontMetrics metr = getFontMetrics(font);

        g.drawString(msg, (WIDTH - metr.stringWidth(msg)) / 2, HEIGHT / 2 - metr.getHeight());
        g.drawString(scoreMsg, (WIDTH - metr.stringWidth(scoreMsg)) / 2, HEIGHT / 2);
        g.drawString(replayMsg, (WIDTH - metr.stringWidth(replayMsg)) / 2, HEIGHT / 2 + metr.getHeight() + 10);
    }


    private void checkApple() {
        Point head = snake.get(0);
        if (head.equals(apple)) {
            System.out.println("Apple eaten!");
            snake.add(new Point(-1, -1)); // Добавление нового элемента к змейке
            score += 2;  // Увеличение счёта
            locateApple(); // Перемещение яблока
            if (score >= 20) {
                inGame = false; // Завершить игру, если счёт достиг или превысил 50
                sendScoreToServer(score); // Отправка счёта на сервер
            }
        }
    }


    private void move() {
        for (int z = snake.size() - 1; z > 0; z--) {
            snake.set(z, new Point(snake.get(z - 1)));
        }

        Point head = snake.get(0);
        if (leftDirection) {
            head.translate(-DOT_SIZE, 0);
        } else if (rightDirection) {
            head.translate(DOT_SIZE, 0);
        } else if (upDirection) {
            head.translate(0, -DOT_SIZE);
        } else if (downDirection) {
            head.translate(0, DOT_SIZE);
        }
        snake.set(0, head);
    }

    private void sendScoreToServer(int score) {
        if (out != null) {
            out.println("SCORE " + playerName + " " + score);
        }
    }


    private void checkCollision() {
        Point head = snake.get(0);
        if (head.x >= WIDTH - 20 || head.x < 0 || head.y >= HEIGHT - 20 || head.y < 0) {
            inGame = false;
            sendScoreToServer(score);
        }

        for (int z = snake.size() - 1; z > 0; z--) {
            if (snake.get(z).equals(head)) {
                inGame = false;
                sendScoreToServer(score);
            }
        }

        if (!inGame) {
            timer.stop();
            repaint(); // Запрашивает перерисовку, gameOver будет вызван в методе paint
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (inGame) {
            checkApple();
            checkCollision();
            move();
        }

        repaint();
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();

            if ((key == KeyEvent.VK_LEFT) && (!rightDirection)) {
                leftDirection = true;
                upDirection = false;
                downDirection = false;
            }

            if ((key == KeyEvent.VK_RIGHT) && (!leftDirection)) {
                rightDirection = true;
                upDirection = false;
                downDirection = false;
            }

            if ((key == KeyEvent.VK_UP) && (!downDirection)) {
                upDirection = true;
                rightDirection = false;
                leftDirection = false;
                downDirection = false;
            }

            if ((key == KeyEvent.VK_DOWN) && (!upDirection)) {
                downDirection = true;
                rightDirection = false;
                leftDirection = false;
            }

            if ((key == KeyEvent.VK_R) && (!inGame)) {
                inGame = true;
                score = 0;
                leftDirection = false;
                rightDirection = true;
                upDirection = false;
                downDirection = false;
                initGame();
            }
        }
    }

    public static void main(String[] args) {
        String playerName = JOptionPane.showInputDialog("Enter your name:");
        EventQueue.invokeLater(() -> {
            JFrame ex = new SnakeGame(playerName);
            ex.setVisible(true);
        });
    }
}
