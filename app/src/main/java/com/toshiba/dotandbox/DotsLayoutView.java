package com.toshiba.dotandbox;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DotsLayoutView extends View {
    private TextView player1score = null;
    private TextView player2score = null;
    private TextView declarewinner;
    private TextView player1name;
    private TextView player2name;
    public PointF startDot;
    public PointF endDot;
    private int rows;// Number of rows
    private int columns; // Number of columns
    private int dotSpacing; // Spacing between dots
    private final List<PointF> dots = new ArrayList<>(); // structure to store dots coordinates
    public int turn = 0;
    public Map<PointF, Set<PointF>> mapOfPointFSets = new HashMap<>(); //structure to store info about which dots are connected
    public Map<Pair<PointF, PointF>, Paint> mapOfPaints = new HashMap<>(); //structure to store brush/color attributes for each line drawn, line is identified by pair of points
    public Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes = new HashMap<>(); //structure to store which all boxes are formed, box is identified by topLeft and bottomRight dots
    public int[] Box = new int[2]; // structure to store boxes formed by two players
    int possible_box_num;
    private Paint dotPaint;
    public Paint player1brush;
    public Paint player2brush;
    private Paint player1box;
    private Paint player2box;
    private Paint borderPaint;
    private boolean singlePlayerGame;
    public boolean computerMoveDone = false;
    public boolean acceptUserInput = true;
    float scale = getResources().getDisplayMetrics().density;
    int pixvalue_250 = (int) (250 * scale + 0.5f);
    int pixvalue_150 = (int) (150 * scale + 0.5f);
    int pixvalue_100 = (int) (100 * scale + 0.5f);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    public SoundPool soundPool;
    public int soundId_action; // To store the ID of the loaded sound
    public int soundId_judge;
    private boolean gameOver = false;
    private FrameLayout player1frame;
    private FrameLayout player2frame;
    private TextView LoadingPlayer1;
    private TextView LoadingPlayer2;
    private Handler handler1;
    private Handler handler2;
    private int dotCount = 0;
    private int dotCount1 = 0;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height-pixvalue_250);  // setting remaining space(excluding top and bottom layouts for score and button display) as dimensions of dotViewLayout
    }
    public DotsLayoutView(Context context) {
        super(context);
        init(context);
    }

    public DotsLayoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DotsLayoutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        dotPaint = new Paint();
        dotPaint.setColor(Color.parseColor("#000000"));
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(4);

        player1brush = new Paint();
        player1brush.setColor(Color.parseColor("#03ad06"));
        player1brush.setStyle(Paint.Style.FILL);
        player1brush.setStrokeWidth(8);

        player2brush = new Paint();
        player2brush.setColor(Color.parseColor("#0407ba"));
        player2brush.setStyle(Paint.Style.FILL);
        player2brush.setStrokeWidth(8);

        player1box = new Paint();
        player1box.setColor(Color.parseColor("#03ad06"));
        //player1box.setStyle(Paint.Style.STROKE);
        float[] intervals = {20, 10};
        player1box.setPathEffect(null);
        player1box.setPathEffect(new android.graphics.DashPathEffect(intervals, 5));
        player1box.setARGB(50, 60, 179, 113);

        player2box = new Paint();
        player2box.setColor(Color.parseColor("#0407ba"));
        //player2box.setStyle(Paint.Style.STROKE);
        player2box.setPathEffect(null);
        player2box.setPathEffect(new android.graphics.DashPathEffect(intervals, 5));
        player2box.setARGB(50, 0, 0, 255);

        borderPaint = new Paint();
        borderPaint.setColor(getResources().getColor(R.color.black)); // Set the border color
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6);

        createNewSoundPool();
        // Load the sound file into the SoundPool
        soundId_action = soundPool.load(this.getContext(), R.raw.cork, 1);
        soundId_judge = soundPool.load(this.getContext(), R.raw.decide, 1);

        handler1 = new Handler(Looper.getMainLooper());
        handler2 = new Handler(Looper.getMainLooper());
        startLoadingAnimationPlayer1();
        startLoadingAnimationPlayer2();
    }

    private void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10) // Maximum number of simultaneous streams
                .setAudioAttributes(attributes)
                .build();
    }

    private void createDots(Canvas canvas)
    {
        int width = getWidth();
        int height = getHeight();
        //Log.d("singleplayer", "init: width="+width);
        //Log.d("singleplayer", "init:height="+height);

        int margin_left = 0;//(width/(columns+1));
        int margin_top = 0;//height/(rows+1); //(height/rows);
        int margin_right = (width/(columns+1));
        int margin_bottom = height/(rows+1); //(height/(rows));

        int horizontalSpacing = (width) / (columns+1);
        int verticalSpacing = (height) / (rows+1);
        if(horizontalSpacing * (rows+1) >= height) //adjusting horizontal spacing according to vertical space needed
        {
            horizontalSpacing = height/(rows+1);
            margin_left = (width - (horizontalSpacing * (columns+1)))/2;
        }
        else{
            int bal_height = height - (horizontalSpacing * (rows+1));
            margin_top += bal_height/2;
        }
        dotSpacing = horizontalSpacing;

        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= columns; col++) {
                int x =  margin_left + (horizontalSpacing * col);
                int y =  margin_top + (horizontalSpacing * row);
                dots.add(new PointF(x, y));
                //Log.d("singleplayer", "onDraw: x="+x+" y="+y);
                // Draw a dot at the calculated position
                // Radius of each dot
                int dotRadius = 10;
                canvas.drawCircle(x, y, dotRadius, dotPaint);
            }
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);
        createDots(canvas);
        if(startDot != null && endDot != null)
        {
            canvas.drawLine(startDot.x, startDot.y, endDot.x, endDot.y, dotPaint);
        }
        if (!mapOfPointFSets.isEmpty()) { //draw lines by checking this map
            Map<PointF, Boolean> visited = new HashMap<>();
            PointF Dot1;

            for (Map.Entry<PointF, Set<PointF>> entry : mapOfPointFSets.entrySet()) {
                Dot1 = entry.getKey();
                visited.put(Dot1, true);
                for(PointF Dot2 : entry.getValue()) {
                    if(!visited.containsKey(Dot2))
                    {
                        Pair<PointF, PointF> DotPair = new Pair<>(Dot1, Dot2);
                        Pair<PointF, PointF> DotPair_rev = new Pair<>(Dot2, Dot1);
                        PointF drawDot1 = new PointF(Dot1.x, Dot1.y);
                        PointF drawDot2 = new PointF(Dot2.x, Dot2.y);
                        getDotsToDraw(drawDot1, drawDot2);
                        if(mapOfPaints.containsKey(DotPair)) {
                            canvas.drawLine(drawDot1.x, drawDot1.y, drawDot2.x, drawDot2.y, mapOfPaints.get(DotPair));
                        }
                        else if(mapOfPaints.containsKey(DotPair_rev)) {
                            canvas.drawLine(drawDot1.x, drawDot1.y, drawDot2.x, drawDot2.y, mapOfPaints.get(DotPair_rev));
                        }
                    }
                }
            }
        }
        if(!mapOfFormedBoxes.isEmpty())  //draw boxes by using data stored in this map
        {
            for(Map.Entry<Pair<PointF, PointF>, Integer> entry : mapOfFormedBoxes.entrySet())
            {
                Pair<PointF, PointF> cords = entry.getKey();
                float left = cords.first.x + (int) (4 * scale + 0.5f);
                float top = cords.first.y + (int) (4 * scale + 0.5f);
                float right = cords.second.x - (int) (4 * scale + 0.5f);
                float bottom = cords.second.y - (int) (4 * scale + 0.5f);

                if(entry.getValue() == 0) //player1 formed this box
                {
                    Path path = new Path();
                    path.addRect(left, top, right, bottom, Path.Direction.CW);
                    // Draw the striped filled rectangle on the canvas
                    canvas.drawPath(path, player1box);
                    //canvas.drawRect(left, top, right, bottom, player1box);
                }
                else{ //player2 formed this box
                    Path path = new Path();
                    path.addRect(left, top, right, bottom, Path.Direction.CW);
                    // Draw the striped filled rectangle on the canvas
                    canvas.drawPath(path, player2box);
                    //canvas.drawRect(left, top, right, bottom, player2box);
                }
            }
        }
        if(player1score != null)
            player1score.setText(Integer.toString(Box[0]));  //display player 1 score in score box
        if(player2score != null)
            player2score.setText(Integer.toString(Box[1])); //display player 2 score in score box
        Log.d("singleplayer", "onDraw : turn%2=" + turn%2 + " computerMoveDone=" + computerMoveDone);
        if(turn%2 == 0)  // player1's turn, highlight and make loading dots visible for player1
        {
            player1frame.setBackgroundResource(R.drawable.tile_highlight);
            player2frame.setBackgroundResource(R.drawable.none);
            LoadingPlayer2.setVisibility(View.INVISIBLE);
            LoadingPlayer1.setVisibility(View.VISIBLE);
        }
        else{ // player2's turn, highlight and make loading dots visible for player2
            player2frame.setBackgroundResource(R.drawable.tile_highlight);
            player1frame.setBackgroundResource(R.drawable.none);
            LoadingPlayer1.setVisibility(View.INVISIBLE);
            LoadingPlayer2.setVisibility(View.VISIBLE);
        }
        isGameOver(); //checking if game is over
        if(turn%2 == 1 && !computerMoveDone && singlePlayerGame)  //when playing with android, if it is player2(android)'s turn and its not made the move yet, call getBestMove()
        {
            executorService.submit(() -> {
                Log.d("singleplayer", "onDraw, executorService : inside computer turn=" + turn);
                Pair<PointF, PointF> choice = getBestMove(mapOfPointFSets, turn, mapOfFormedBoxes, Box);
                Log.d("singleplayer", "onDraw, executorService : after getBestMove x=" + choice.first.x + " y=" + choice.first.y);
                startDot = choice.first;
                endDot = choice.second;
                connectDotsinMap(startDot, endDot, mapOfPointFSets);
                mapOfPaints.put(new Pair<>(startDot, endDot), turn % 2 == 0 ? player1brush : player2brush);
                Log.d("singleplayer", "onDraw, executorService : before calling joinDots for computer");
                turn = joinDots(mapOfPointFSets, startDot, endDot, turn, false, mapOfFormedBoxes, Box, mapOfPaints);
                Log.d("singleplayer", "onDraw, executorService : after calling joinDots for computer");
                turn++;
                // Play the sound
                soundPool.play(soundId_action, 1.0f, 1.0f, 1, 0, 1.0f);
                if (turn % 2 == 0) {  //turn incremented and user's turn now
                    computerMoveDone = true;
                    acceptUserInput = true;
                }
                Log.d("singleplayer", "turn=" + turn + " computerMoveDone=" + computerMoveDone + " acceptUserInput=" + acceptUserInput);
                startDot = null;
                endDot = null;
                invalidate();
                if (isGameOver())
                    ;
            });
        }
    }

    //this function is a helper for display beautification, to draw the line little short so as to not overlap the dot display
    private void getDotsToDraw(PointF p1, PointF p2) {
        if(isHorizontalLine(p1, p2))
        {
            if(p1.x < p2.x)
            {
                p1.x += (int) (4 * scale + 0.5f);
                p2.x -= (int) (4 * scale + 0.5f);
            }
            else{
                p2.x += (int) (4 * scale + 0.5f);
                p1.x -= (int) (4 * scale + 0.5f);
            }
        }
        else{
            if(p1.y < p2.y)
            {
                p1.y += (int) (4 * scale + 0.5f);
                p2.y -= (int) (4 * scale + 0.5f);
            }
            else{
                p2.y += (int) (4 * scale + 0.5f);
                p1.y -= (int) (4 * scale + 0.5f);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if(acceptUserInput) { //process touch events from user only when acceptUserInput is true, this will block unnecessary touches by user when android is playing or when game is over
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startDot = findNearestDot(x, y);
                    Log.d("onTouchEvent", "onTouchEvent:  MotionEvent.ACTION_DOWN " + startDot);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Handle line drawing as the user moves their finger
                    endDot = new PointF(x, y);
                    Log.d("onTouchEvent", "onTouchEvent:  MotionEvent.ACTION_MOVE " + endDot);
                    invalidate(); // Redraw the lines
                    break;
                case MotionEvent.ACTION_UP:
                    endDot = findNearestDot(x, y);
                    Log.d("onTouchEvent", "onTouchEvent:  MotionEvent.ACTION_UP " + endDot);
                    if (startDot != null && endDot != null) {
                        if (!singlePlayerGame) { //play with friend
                            // logic to join the dots
                            if (canBeJoined(startDot, endDot, mapOfPointFSets)) {
                                connectDotsinMap(startDot, endDot, mapOfPointFSets);
                                mapOfPaints.put(new Pair<>(startDot, endDot), turn % 2 == 0 ? player1brush : player2brush);
                                turn = joinDots(mapOfPointFSets, startDot, endDot, turn, false, mapOfFormedBoxes, Box, mapOfPaints); // this will check for any box formation
                                turn++;
                                // Play the sound
                                soundPool.play(soundId_action, 1.0f, 1.0f, 1, 0, 1.0f);
                            }
                            startDot = null;
                            endDot = null;
                            invalidate();
                            isGameOver();
                        } else {  // play with android
                            if (turn % 2 == 0) {
                                if (canBeJoined(startDot, endDot, mapOfPointFSets)) {
                                    connectDotsinMap(startDot, endDot, mapOfPointFSets);
                                    mapOfPaints.put(new Pair<>(startDot, endDot), turn % 2 == 0 ? player1brush : player2brush);
                                    Log.d("singleplayer", "in onTouchEvent() before calling joinDots for user");
                                    turn = joinDots(mapOfPointFSets, startDot, endDot, turn, false, mapOfFormedBoxes, Box, mapOfPaints); // this will check for any box formation and calls auto box form for android if user drew a line such that next player can complete the box
                                    Log.d("singleplayer", "in onTouchEvent() after calling joinDots for user");
                                    turn++;
                                    // Play the sound
                                    soundPool.play(soundId_action, 1.0f, 1.0f, 1, 0, 1.0f);
                                    if (turn % 2 == 1) {
                                        computerMoveDone = false;
                                        acceptUserInput = false;
                                    }
                                    Log.d("singleplayer", "onTouchEvent : after user invalidate turn=" + turn);

                                }
                                startDot = null;
                                endDot = null;
                                invalidate();
                                isGameOver();
                            }
                        }
                    }
                    break;
            }
            super.onTouchEvent(event);
        }
        return true;
    }
    public boolean isGameOver() {
        possible_box_num = (rows-1) * (columns-1);
        Log.d("singleplayer", "isGameOver: Box[0]="+Box[0]+" Box[1]=" + Box[1] + " possible_box_num="+possible_box_num);
        if(Box[0] + Box[1] == possible_box_num)
        {
            if(Box[0] > Box[1])
            {
                Log.d("turns", "isGameOver : Player1 won!! with boxes="+Box[0]);
                declarewinner.setText("Player1 has won!!");
                declarewinner.setBackgroundResource(R.drawable.tile_background);
                player1name.setBackgroundResource(R.drawable.tile_winner);
            }
            else if(Box[0] < Box[1])
            {
                Log.d("turns", "isGameOver : Player2 won!! with boxes="+Box[1]);
                declarewinner.setText("Player2 has won!!");
                declarewinner.setBackgroundResource(R.drawable.tile_background);
                player2name.setBackgroundResource(R.drawable.tile_winner);
            }
            else{
                Log.d("turns", "isGameOver : It's a Draw! with boxes each = "+Box[0]);
                declarewinner.setText("It's a Draw!");
                declarewinner.setBackgroundResource(R.drawable.tile_background);
            }
            computerMoveDone = true;
            acceptUserInput = false;
            // Play the sound
            if(!gameOver)
                soundPool.play(soundId_judge, 1.0f, 1.0f, 1, 0, 1.0f);
            gameOver = true;
            stopLoadingAnimationPlayer1();
            stopLoadingAnimationPlayer2();
            LoadingPlayer1.setVisibility(View.INVISIBLE);
            LoadingPlayer2.setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    /*
    * This function
    * 1. checks if any box is formed from last line drawn, if so updates mapOfFormedBoxes and Box array
    * 2. If game mode is PlayWithAndroid,
    *       1. If this function is called from minimax() algorithm,
    *           if box formed, then call dominoFill() for the current player to check and fill other possible boxes
    *           if box can be formed in next move, then call dominoFill() for the next player to check and fill other possible boxes
    *       2. Else,
    *           if box formed, then call dominoFill() for the current player to check and fill other possible boxes, only if current player is Android
    *           if box can be formed in next move, then call dominoFill() for Android player to check and fill other possible boxes, only if current player is User
    * */
    public Integer joinDots(Map<PointF, Set<PointF>> mapOfPointFSets_copy, PointF A, PointF B, int turn_copy, boolean fromMiniMax, Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_copy, int[] Box_copy, Map<Pair<PointF, PointF>, Paint> mapOfPaints_copy)
    {
        //Log.d("singleplayer", "joinDots entry");
        int joined1 = 0, joined2 = 0;
        boolean boxFormed = false;
        if(isHorizontalLine(A, B))
        {
            if(A.x > B.x)
            {
                PointF temp = A;
                A = B;
                B = temp;
            }
            PointF up1 = new PointF(A.x, A.y-dotSpacing);
            PointF up2 = new PointF(B.x, B.y-dotSpacing);
            if(dots.contains(up1) && dots.contains(up2))
            {
                if(mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(up1))
                        joined1 += 1;
                if(mapOfPointFSets_copy.containsKey(up1) && mapOfPointFSets_copy.get(up1).contains(up2))
                        joined1 += 1;
                if(mapOfPointFSets_copy.containsKey(up2) && mapOfPointFSets_copy.get(up2).contains(B))
                        joined1 += 1;
                if(joined1 == 3)
                {
                    Box_copy[turn_copy%2]++;
                    boxFormed = true;
                    mapOfFormedBoxes_copy.put(new Pair<>(up1, B), turn_copy%2);
                }
            }

            PointF bot1 = new PointF(A.x, A.y+dotSpacing);
            PointF bot2 = new PointF(B.x, B.y+dotSpacing);
            if(dots.contains(bot1) && dots.contains(bot2))
            {
                if(mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(bot1))
                    joined2 += 1;
                if(mapOfPointFSets_copy.containsKey(bot1) && mapOfPointFSets_copy.get(bot1).contains(bot2))
                    joined2 += 1;
                if(mapOfPointFSets_copy.containsKey(bot2) && mapOfPointFSets_copy.get(bot2).contains(B))
                    joined2 += 1;

                if(joined2 == 3)
                {
                    Box_copy[turn_copy%2]++;
                    boxFormed = true;
                    mapOfFormedBoxes_copy.put(new Pair<>(A, bot2), turn_copy%2);
                }
            }

        }
        else if(isVerticalLine(A, B))
        {
            if(A.y > B.y)
            {
                PointF temp = A;
                A = B;
                B = temp;
            }
            PointF left1 = new PointF(A.x-dotSpacing, A.y);
            PointF left2 = new PointF(B.x-dotSpacing, B.y);
            if(dots.contains(left1) && dots.contains(left2))
            {
                if(mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(left1))
                    joined1 += 1;
                if(mapOfPointFSets_copy.containsKey(left1) && mapOfPointFSets_copy.get(left1).contains(left2))
                    joined1 += 1;
                if(mapOfPointFSets_copy.containsKey(left2) && mapOfPointFSets_copy.get(left2).contains(B))
                    joined1 += 1;

                if(joined1 == 3)
                {
                    Box_copy[turn_copy%2]++;
                    boxFormed = true;
                    mapOfFormedBoxes_copy.put(new Pair<>(left1, B), turn_copy%2);
                }
            }

            PointF right1 = new PointF(A.x+dotSpacing, A.y);
            PointF right2 = new PointF(B.x+dotSpacing, B.y);
            if(dots.contains(right1) && dots.contains(right2))
            {
                if(mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(right1))
                    joined2 += 1;
                if(mapOfPointFSets_copy.containsKey(right1) && mapOfPointFSets_copy.get(right1).contains(right2))
                    joined2 += 1;
                if(mapOfPointFSets_copy.containsKey(right2) && mapOfPointFSets_copy.get(right2).contains(B))
                    joined2 += 1;

                if(joined2 == 3)
                {
                    Box_copy[turn_copy%2]++;
                    boxFormed = true;
                    mapOfFormedBoxes_copy.put(new Pair<>(A, right2), turn_copy%2);
                }
            }
        }
        if(boxFormed)
        {
            if(singlePlayerGame && fromMiniMax)
            {
                dominoFill(mapOfPointFSets_copy, A, B, turn_copy, mapOfFormedBoxes_copy, Box_copy, mapOfPaints_copy);
            }
            else if(singlePlayerGame && !fromMiniMax && turn_copy%2 != 0)
            {
                dominoFill(mapOfPointFSets_copy, A, B, turn_copy, mapOfFormedBoxes_copy, Box_copy, mapOfPaints_copy);
            }
            turn_copy++;
        }
        else if(joined1 == 2 || joined2 == 2)
        {
                if(singlePlayerGame && fromMiniMax)
                {
                    dominoFill(mapOfPointFSets_copy, A, B, turn_copy+1, mapOfFormedBoxes_copy, Box_copy, mapOfPaints_copy);
                }
                else if (singlePlayerGame && !fromMiniMax && turn_copy%2 != 1)
                {
                    dominoFill(mapOfPointFSets_copy, A, B, turn_copy+1, mapOfFormedBoxes_copy, Box_copy, mapOfPaints_copy);
                }
        }
        //Log.d("singleplayer", "joinDots exit");
        return turn_copy;
    }

    /*This function checks and returns the best choice for the next move of Android in SinglePlayer gameMode
    *   It will first find all possible lines that are pending to draw in the grid, and calls minimax() for each of those, gets the score and compares with the scores achieved by drawing
    *   other lines and decides best choice based on max score that will be achieved.
    * */
    public Pair<PointF, PointF> getBestMove(Map<PointF, Set<PointF>> mapOfPointFSets_copy, Integer turn_copy, Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_copy, int[] Box_copy)
    {
        Log.d("singleplayer", "getBestMove entry");
        Map<PointF, Set<PointF>> mapOfPointFSets_org = copyOfmapOfPointFSets(mapOfPointFSets_copy);
        Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_org = copyOfmapOfFormedBoxes(mapOfFormedBoxes_copy);
        int[] Box_org = copyOfBoxarray(Box_copy);
        int turn_org = turn_copy;
        int depth = 2;
        Pair<PointF,PointF> ans = null;
        int score = Integer.MIN_VALUE;
        int bestScore = Integer.MIN_VALUE;
        Log.d("singleplayer", "getBestMove: before for loop turn_copy=" + turn_copy);
        Map<Pair<PointF, PointF>, Boolean> possible_lines = new HashMap();

        int i = 0;
        for(int x = 0; x < rows; x++) {
            for (int y = 0; y < columns - 1; y++) {

                PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                PointF nextDot = new PointF(dot.x+dotSpacing, dot.y);
                if(!isConnected(dot, nextDot, mapOfPointFSets_copy))
                {
                    possible_lines.put(new Pair(dot, nextDot), false);
                }
                i++;
            }
            i++;
        }
        //Log.d("singleplayer", "getBestMove: after first for loop bestScore=" + bestScore);
        i = 0;
        for(int x = 0; x < rows-1; x++) {
            for (int y = 0; y < columns; y++) {

                PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                PointF nextDot = new PointF(dot.x, dot.y+dotSpacing);

                if(!isConnected(dot, nextDot, mapOfPointFSets_copy))
                {
                    possible_lines.put(new Pair(dot, nextDot), false);
                }
                i++;
            }
        }
        int total_lines_count = ((columns-1) * rows) + ((rows-1) * columns);
        int pending_lines_count = possible_lines.size();
        //depth = total_lines_count - pending_lines_count + 1;
        if(pending_lines_count < total_lines_count/2)
        {
            depth = 3;   // increase depth to 3 when remaining lines to be filled is less than half of total lines possible, so that better decision will be taken by Android
        }
        Log.d("singleplayer", "getBestMove: total_lines_count=" + total_lines_count+ " pending_lines_count="+pending_lines_count+" depth="+depth);
        Map.Entry<Pair<PointF, PointF>, Boolean> entryRand = getRandomEntry(possible_lines);
        if(entryRand != null)
        {
            PointF dot = entryRand.getKey().first;
            PointF nextDot = entryRand.getKey().second;

            Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
            Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
            int[] Box_local = copyOfBoxarray(Box_org);
            int turn_local = turn_org;

            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth);
            //Log.d("singleplayer", "getBestMove: score=" + score);
            if(score > bestScore)
            {
                bestScore = score;
                ans = new Pair<PointF, PointF>(dot, nextDot);
            }
            if(bestScore >= (possible_box_num/2))
            {
                return ans;
            }
            possible_lines.remove(entryRand.getKey());
        }

        for (Map.Entry<Pair<PointF, PointF>, Boolean> entry : possible_lines.entrySet()) {
            PointF dot = entry.getKey().first;
            PointF nextDot = entry.getKey().second;

            Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
            Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
            int[] Box_local = copyOfBoxarray(Box_org);
            int turn_local = turn_org;

            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth);
            //Log.d("singleplayer", "getBestMove: score=" + score);
            if(score > bestScore)
            {
                bestScore = score;
                ans = new Pair<PointF, PointF>(dot, nextDot);
            }
            if(bestScore >= (possible_box_num/2))
            {
                return ans;
            }
            entry.setValue(true);
        }
/*
        i = 0;
        for(int x = 0; x < rows; x++) {
            for (int y = 0; y < columns - 1; y++) {
                Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                int[] Box_local = copyOfBoxarray(Box_org);
                int turn_local = turn_org;

                PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                PointF nextDot = new PointF(dot.x+dotSpacing, dot.y);
                if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                {
                    connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                    Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                    turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                    score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth);
                    if(score > bestScore)
                    {
                        bestScore = score;
                        ans = new Pair<PointF, PointF>(dot, nextDot);
                    }
                }
                i++;
            }
            i++;
        }
        Log.d("singleplayer", "getBestMove: after first for loop bestScore=" + bestScore);
        i = 0;
        for(int x = 0; x < rows-1; x++) {
            for (int y = 0; y < columns; y++) {
                Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                int[] Box_local = copyOfBoxarray(Box_org);
                int turn_local = turn_org;

                PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                PointF nextDot = new PointF(dot.x, dot.y+dotSpacing);

                if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                {
                    connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                    Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                    turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                    score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth);
                    if(score > bestScore)
                    {
                        bestScore = score;
                        ans = new Pair<PointF, PointF>(dot, nextDot);
                    }
                }
                i++;
            }
        }*/
        Log.d("singleplayer", "getBestMove exit");
        return ans;
    }
    private static <K, V> Map.Entry<K, V> getRandomEntry(Map<K, V> map) {
        List<Map.Entry<K, V>> entryList = new ArrayList<>(map.entrySet());
        // Use Random to select a random index
        Random random = new Random();
        int randomIndex = random.nextInt(entryList.size());

        // Return the randomly selected entry
        return entryList.get(randomIndex);
    }
    private int minimax(Map<PointF, Set<PointF>> mapOfPointFSets_copy, Integer turn_copy, Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_copy, int[] Box_copy, int depth)
    {
        //Log.d("singleplayer", "minimax: entry");
        if(Box_copy[0] + Box_copy[1] == possible_box_num)
        {
            //Log.d("singleplayer", "minimax: exit");
            if(Box_copy[0] > Box_copy[1])
            {
                return -1 * (Box_copy[0]);
            }
            else if(Box_copy[0] < Box_copy[1])
            {
                return (Box_copy[1]);
            }
            else{
                return 0;
            }
        }
        if(depth > 0)
        {
            Map<PointF, Set<PointF>> mapOfPointFSets_org = copyOfmapOfPointFSets(mapOfPointFSets_copy);
            Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_org = copyOfmapOfFormedBoxes(mapOfFormedBoxes_copy);
            int[] Box_org = copyOfBoxarray(Box_copy);
            int turn_org = turn_copy;

            if(turn_copy%2 != 0) // Android's turn
            {
                int score = 0, bestScore = Integer.MIN_VALUE;
                int i = 0;
                for(int x = 0; x < rows; x++)
                {
                    for (int y = 0; y < columns - 1; y++)
                    {
                        Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                        Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                        int[] Box_local = copyOfBoxarray(Box_org);
                        int turn_local = turn_org;

                        PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                        PointF nextDot = new PointF(dot.x+dotSpacing, dot.y);
                        if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                        {
                            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth-1);
                            bestScore = Math.max(bestScore, score);
                        }
                        i++;
                    }
                    i++;
                }

                i = 0;
                for(int x = 0; x < rows-1; x++) {
                    for (int y = 0; y < columns; y++) {
                        Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                        Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                        int[] Box_local = copyOfBoxarray(Box_org);
                        int turn_local = turn_org;

                        PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                        PointF nextDot = new PointF(dot.x, dot.y+dotSpacing);

                        if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                        {
                            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth-1);
                            bestScore = Math.max(bestScore, score);
                        }
                        i++;
                    }
                }
                return bestScore;
            }
            else{   // User's turn
                int score = 0, bestScore = Integer.MAX_VALUE;
                int i = 0;
                for(int x = 0; x < rows; x++)
                {
                    for (int y = 0; y < columns - 1; y++)
                    {
                        Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                        Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                        int[] Box_local = copyOfBoxarray(Box_org);
                        int turn_local = turn_org;

                        PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                        PointF nextDot = new PointF(dot.x+dotSpacing, dot.y);
                        if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                        {
                            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth-1);
                            bestScore = Math.min(bestScore, score);
                        }
                        i++;
                    }
                    i++;
                }

                i = 0;
                for(int x = 0; x < rows-1; x++) {
                    for (int y = 0; y < columns; y++) {
                        Map<PointF, Set<PointF>> mapOfPointFSets_local = copyOfmapOfPointFSets(mapOfPointFSets_org);
                        Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_local = copyOfmapOfFormedBoxes(mapOfFormedBoxes_org);
                        int[] Box_local = copyOfBoxarray(Box_org);
                        int turn_local = turn_org;

                        PointF dot = new PointF(dots.get(i).x, dots.get(i).y);
                        PointF nextDot = new PointF(dot.x, dot.y+dotSpacing);

                        if(!isConnected(dot, nextDot, mapOfPointFSets_local))
                        {
                            connectDotsinMap(dot, nextDot, mapOfPointFSets_local);
                            Map<Pair<PointF, PointF>, Paint> dummy = new HashMap<>();
                            turn_local = joinDots(mapOfPointFSets_local, dot, nextDot, turn_local, true, mapOfFormedBoxes_local, Box_local, dummy);
                            score = minimax(mapOfPointFSets_local, turn_local+1, mapOfFormedBoxes_local, Box_local, depth-1);
                            bestScore = Math.min(bestScore, score);
                        }
                        i++;
                    }
                }
                return bestScore;
            }
        }
        else{
            //Log.d("singleplayer", "minimax: exit");
            if(Box_copy[0] > Box_copy[1])
            {
                return -1 * (Box_copy[0]);
            }
            else if(Box_copy[0] < Box_copy[1])
            {
                return (Box_copy[1]);
            }
            else{
                return 0;
            }
        }
    }
    private  Map<PointF, Set<PointF>> copyOfmapOfPointFSets(Map<PointF, Set<PointF>> orgMap)
    {
        Map<PointF, Set<PointF>> copyMap = new HashMap<>();
        for (Map.Entry<PointF, Set<PointF>> entry : orgMap.entrySet())
        {
            PointF A = new PointF(entry.getKey().x, entry.getKey().y);
            Set<PointF> valueSet = new HashSet<>();
            for(PointF Dot : entry.getValue())
            {
               valueSet.add(new PointF(Dot.x, Dot.y));
            }
            copyMap.put(A, valueSet);
        }
        return copyMap;
    }
    private Map<Pair<PointF, PointF>, Integer> copyOfmapOfFormedBoxes(Map<Pair<PointF, PointF>, Integer> orgMap)
    {
        Map<Pair<PointF, PointF>, Integer> copyMap = new HashMap<>();
        for (Map.Entry<Pair<PointF, PointF>, Integer> entry : orgMap.entrySet())
        {
            Pair<PointF, PointF> newpair = new Pair<>(entry.getKey().first, entry.getKey().second);
            Integer val = new Integer(Integer.valueOf(entry.getValue()));
            copyMap.put(newpair, val);
        }
        return copyMap;
    }
    private int[] copyOfBoxarray(int[] orgArray)
    {
        int[] copyArray = new int[orgArray.length];
        System.arraycopy(orgArray, 0, copyArray, 0, orgArray.length);
        return copyArray;
    }

    /*
        This function just finds all possible lines by drawing which a box can be formed and draws it for that player.
    */
    private void dominoFill(Map<PointF, Set<PointF>> mapOfPointFSets_copy, PointF A, PointF B, int turn_copy, Map<Pair<PointF, PointF>, Integer> mapOfFormedBoxes_copy, int[] Box_copy,  Map<Pair<PointF, PointF>, Paint> mapOfPaints_copy)
    {
        //Log.d("singleplayer", "dominoFill entry()");
        PointF next1 = new PointF();
        PointF next2 = new PointF();
        Queue<Pair<PointF, PointF>> pairQueue = new LinkedList<>();
        pairQueue.add(new Pair<>(A, B));

        while(!pairQueue.isEmpty())
        {
            Pair<PointF, PointF> pair = pairQueue.poll();
            PointF dot1 = pair.first;
            PointF dot2 = pair.second;
            int joined1 = 0, joined2 = 0;
            if(isHorizontalLine(dot1, dot2))
            {
                if(dot1.x > dot2.x)
                {
                    PointF temp = dot1;
                    dot1 = dot2;
                    dot2 = temp;
                }
                PointF up1 = new PointF(dot1.x, dot1.y-dotSpacing);
                PointF up2 = new PointF(dot2.x, dot2.y-dotSpacing);
                if(dots.contains(up1) && dots.contains(up2))
                {
                    if(isConnected(dot1, up1, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(isConnected(up1, up2, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(isConnected(up2, dot2, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(joined1 == 2)
                    {
                        if(canBeJoined(dot1, up1, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(dot1, up1, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(dot1, up1), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = dot1;
                            next2 = up1;
                        }
                        else if(canBeJoined(up1, up2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(up1, up2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(up1, up2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = up1;
                            next2 = up2;
                        }
                        else if(canBeJoined(up2, dot2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(up2, dot2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(up2, dot2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = up2;
                            next2 = dot2;
                        }
                        Box_copy[turn_copy%2]++;
                        pairQueue.add(new Pair<>(next1, next2));
                        mapOfFormedBoxes_copy.put(new Pair<>(up1, dot2), turn_copy%2);
                    }
                    else if(joined1 == 3 && !mapOfFormedBoxes_copy.containsKey(new Pair<>(up1, dot2)))
                    {
                        Box_copy[turn_copy%2]++;
                        mapOfFormedBoxes_copy.put(new Pair<>(up1, dot2), turn_copy%2);
                    }
                }
                PointF bot1 = new PointF(dot1.x, dot1.y+dotSpacing);
                PointF bot2 = new PointF(dot2.x, dot2.y+dotSpacing);
                if(dots.contains(bot1) && dots.contains(bot2))
                {
                    if(isConnected(dot1, bot1, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(isConnected(bot1, bot2, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(isConnected(dot2, bot2, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(joined2 == 2)
                    {
                        if(canBeJoined(dot1, bot1, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(dot1, bot1, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(dot1, bot1), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = dot1;
                            next2 = bot1;
                        }
                        else if(canBeJoined(bot1, bot2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(bot1, bot2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(bot1, bot2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = bot1;
                            next2 = bot2;
                        }
                        else if(canBeJoined(dot2, bot2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(dot2, bot2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(dot2, bot2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = dot2;
                            next2 = bot2;
                        }
                        Box_copy[turn_copy%2]++;
                        pairQueue.add(new Pair<>(next1, next2));
                        mapOfFormedBoxes_copy.put(new Pair<>(dot1, bot2), turn_copy%2);
                    }
                    else if(joined2 == 3 && !mapOfFormedBoxes_copy.containsKey(new Pair<>(dot1, bot2)))
                    {
                        Box_copy[turn_copy%2]++;
                        mapOfFormedBoxes_copy.put(new Pair<>(dot1, bot2), turn_copy%2);
                    }
                }
            }
            if(isVerticalLine(dot1, dot2))
            {
                if(dot1.y > dot2.y)
                {
                    PointF temp = dot1;
                    dot1 = dot2;
                    dot2 = temp;
                }
                PointF left1 = new PointF(dot1.x-dotSpacing, dot1.y);
                PointF left2 = new PointF(dot2.x-dotSpacing, dot2.y);
                if(dots.contains(left1) && dots.contains(left2))
                {
                    if(isConnected(dot1, left1, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(isConnected(left1, left2, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(isConnected(left2, dot2, mapOfPointFSets_copy))
                        joined1 += 1;
                    if(joined1 == 2)
                    {
                        if(canBeJoined(dot1, left1, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(dot1, left1, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(dot1, left1), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = dot1;
                            next2 = left1;
                        }
                        else if(canBeJoined(left1, left2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(left1, left2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(left1, left2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = left1;
                            next2 = left2;
                        }
                        else if(canBeJoined(left2, dot2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(left2, dot2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(left2, dot2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = left2;
                            next2 = dot2;
                        }
                        Box_copy[turn_copy%2]++;
                        pairQueue.add(new Pair<>(next1, next2));
                        mapOfFormedBoxes_copy.put(new Pair<>(left1, dot2), turn_copy%2);
                    }
                    else if(joined1 == 3 && !mapOfFormedBoxes_copy.containsKey(new Pair<>(left1, dot2)))
                    {
                        Box_copy[turn_copy%2]++;
                        mapOfFormedBoxes_copy.put(new Pair<>(left1, dot2), turn_copy%2);
                    }
                }
                PointF right1 = new PointF(dot1.x+dotSpacing, dot1.y);
                PointF right2 = new PointF(dot2.x+dotSpacing, dot2.y);
                if(dots.contains(right1) && dots.contains(right2))
                {
                    if(isConnected(dot1, right1, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(isConnected(right1, right2, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(isConnected(right2, dot2, mapOfPointFSets_copy))
                        joined2 += 1;
                    if(joined2 == 2)
                    {
                        if(canBeJoined(dot1, right1, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(dot1, right1, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(dot1, right1), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = dot1;
                            next2 = right1;
                        }
                        else if(canBeJoined(right1, right2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(right1, right2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(right1, right2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = right1;
                            next2 = right2;
                        }
                        else if(canBeJoined(right2, dot2, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(right2, dot2, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(right2, dot2), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = right2;
                            next2 = dot2;
                        }
                        Box_copy[turn_copy%2]++;
                        pairQueue.add(new Pair<>(next1, next2));
                        mapOfFormedBoxes_copy.put(new Pair<>(dot1, right2), turn_copy%2);
                    }
                    else if(joined2 == 3 && !mapOfFormedBoxes_copy.containsKey(new Pair<>(dot1, right2)))
                    {
                        Box_copy[turn_copy%2]++;
                        mapOfFormedBoxes_copy.put(new Pair<>(dot1, right2), turn_copy%2);
                    }
                }
            }
        }
        int i = 0;
        for(int x = 0; x < rows-1; x++)
        {
            for(int y = 0; y < columns-1; y++)
            {
                PointF topleft = dots.get(i);
                PointF bottomright = new PointF(topleft.x+dotSpacing, topleft.y+dotSpacing);
                if(!mapOfFormedBoxes_copy.containsKey(new Pair<>(topleft, bottomright)))
                {
                    PointF topright = new PointF(topleft.x+dotSpacing, topleft.y);
                    PointF bottomleft = new PointF(topleft.x, topleft.y+dotSpacing);
                    int joined = 0;
                    if(isConnected(topleft, topright, mapOfPointFSets_copy))
                        joined++;
                    if(isConnected(topleft, bottomleft, mapOfPointFSets_copy))
                        joined++;
                    if(isConnected(topright, bottomright, mapOfPointFSets_copy))
                        joined++;
                    if(isConnected(bottomleft, bottomright, mapOfPointFSets_copy))
                        joined++;
                    if(joined == 3)
                    {
                        if(canBeJoined(topleft, topright, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(topleft, topright, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(topleft, topright), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = topleft;
                            next2 = topright;
                        }
                        else if(canBeJoined(topleft, bottomleft, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(topleft, bottomleft, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(topleft, bottomleft), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = topleft;
                            next2 = bottomleft;
                        }
                        else if(canBeJoined(topright, bottomright, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(topright, bottomright, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(topright, bottomright), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = topright;
                            next2 = bottomright;
                        }
                        else if(canBeJoined(bottomleft, bottomright, mapOfPointFSets_copy))
                        {
                            connectDotsinMap(bottomleft, bottomright, mapOfPointFSets_copy);
                            mapOfPaints_copy.put(new Pair<>(bottomleft, bottomright), turn_copy%2 == 0? player1brush : player2brush);
                            next1 = bottomleft;
                            next2 = bottomright;
                        }
                        Box_copy[turn_copy%2]++;
                        mapOfFormedBoxes_copy.put(new Pair<>(topleft, bottomright), turn_copy%2);
                        dominoFill(mapOfPointFSets_copy, next1, next2, turn_copy, mapOfFormedBoxes_copy, Box_copy, mapOfPaints_copy);
                    }
                }
                i++;
            }
            i++;
        }
        //Log.d("singleplayer", "dominoFill exit()");
    }
    private boolean isConnected(PointF A, PointF B, Map<PointF, Set<PointF>> mapOfPointFSets_copy)
    {
        if (mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(B))
        {
            return true;
        }
        return false;
    }
    public void connectDotsinMap(PointF A, PointF B, Map<PointF, Set<PointF>> mapOfPointFSets_copy)
    {
        if(!mapOfPointFSets_copy.containsKey(A))
        {
            Set<PointF> valueSet1 = new HashSet<>();
            valueSet1.add(new PointF(B.x, B.y));
            mapOfPointFSets_copy.put(new PointF(A.x, A.y), valueSet1);
        } else {
            mapOfPointFSets_copy.get(A).add(new PointF(B.x, B.y));
        }
        if(!mapOfPointFSets_copy.containsKey(B))
        {
            Set<PointF> valueSet1 = new HashSet<>();
            valueSet1.add(new PointF(A.x, A.y));
            mapOfPointFSets_copy.put(B, valueSet1);
        } else {
            mapOfPointFSets_copy.get(B).add(new PointF(A.x, A.y));
        }
    }
    private boolean isVerticalLine(PointF a, PointF b) {
        return (a.x == b.x);
    }

    private boolean isHorizontalLine(PointF a, PointF b) {
        return (a.y == b.y);
    }

    private PointF findNearestDot(float x, float y) {
        // Find the nearest DotView based on user's touch position
        // logic to determine the closest dot
        PointF near = new PointF(Integer.MAX_VALUE, Integer.MAX_VALUE);
        float min_diff = Integer.MAX_VALUE;
        for(PointF p : dots)
        {
            if((Math.abs(x-p.x) + Math.abs(y-p.y)) < min_diff)
            {
                min_diff = (Math.abs(x-p.x) + Math.abs(y-p.y));
                near = p;
            }
        }
        return near;
    }
    /*
    * This function returns true if
    *       given dots are not already joined and they are horizontal or vertical neighbours.
    * */
    private boolean canBeJoined(PointF A, PointF B, Map<PointF, Set<PointF>> mapOfPointFSets_copy){
        if (mapOfPointFSets_copy.containsKey(A) && mapOfPointFSets_copy.get(A).contains(B))
        {
            return false;
        }
        if(A.x == B.x && Math.abs(A.y-B.y) == dotSpacing)
        {
            return true;
        }
        else if(A.y == B.y && Math.abs(A.x-B.x) == dotSpacing)
        {
            return true;
        }
        return false;
    }
    public void setviews(TextView p1score, TextView p2score, TextView dw, TextView p1name, TextView p2name, FrameLayout fm1, FrameLayout fm2, TextView lp1, TextView lp2) {
        player1score = p1score;
        player2score = p2score;
        declarewinner = dw;
        player1name = p1name;
        player2name = p2name;
        player1frame = fm1;
        player2frame = fm2;
        LoadingPlayer1 = lp1;
        LoadingPlayer2 = lp2;
    }
    public void setValues(boolean singlePlayer, int r, int c)
    {
        singlePlayerGame = singlePlayer;
        rows = r;
        columns = c;
    }
    private void startLoadingAnimationPlayer1() {
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                StringBuilder loadingText = new StringBuilder();
                for (int i = 0; i < dotCount; i++) {
                    loadingText.append(".");
                }
                LoadingPlayer1.setText(loadingText.toString());
                // Increment the dot count and reset if it exceeds 3
                dotCount = (dotCount + 1) % 4;

                startLoadingAnimationPlayer1(); // Continue the animation loop
            }
        }, 500); // Set the interval between updates (500 milliseconds)
    }

    private void startLoadingAnimationPlayer2() {
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                StringBuilder loadingText = new StringBuilder();
                for (int i = 0; i < dotCount1; i++) {
                    loadingText.append(".");
                }
                LoadingPlayer2.setText(loadingText.toString());
                // Increment the dot count and reset if it exceeds 3
                dotCount1 = (dotCount1 + 1) % 4;

                startLoadingAnimationPlayer2(); // Continue the animation loop
            }
        }, 500); // Set the interval between updates (500 milliseconds)
    }
    private void stopLoadingAnimationPlayer1()
    {
        handler1.removeCallbacksAndMessages(null);
    }
    private void stopLoadingAnimationPlayer2()
    {
        handler2.removeCallbacksAndMessages(null);
    }
}


