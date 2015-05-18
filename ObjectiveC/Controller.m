//
//  Controller.m
//
//  Copyright (c) 2011-2015 GBKSoft. All rights reserved.
//

#import "FoldPuzzle.h"
#import "SplitPuzzle.h"
#import "ResizeImageClass.h"
#import "MBProgressHUD.h"
#import "networkCheck.h"
#import "HexColor.h"
#import "TakePhotoPuzzle.h"
#import "AppSettings.h"
#import "AppDelegate.h"
#import "APIServerPart.h"
#import "GBKDB.h"
#import "GBKApi.h"

#import <QuartzCore/QuartzCore.h>

#define IDIOM    UI_USER_INTERFACE_IDIOM()
#define IPAD     UIUserInterfaceIdiomPad

//---------------------------------------------------
@interface FoldPuzzle (){

    NSMutableArray * splittedImages;
    NSMutableArray * shuffleImages;
    NSMutableArray * gridLayer;
    NSMutableArray * centerParts;
    NSMutableArray * centerFrames;
    NSMutableArray * arrayPositionsSavedState;

    NSArray * rotatesForShuffle;

    NSTimer * myTimer;

    int currentTime;
    int actionState;

    BOOL isMoved;
    BOOL endFold;
    BOOL removePuzzle;
    BOOL losePuzzle;
    BOOL isFirstPuzzle;
    BOOL isOldCompleted;

    int startFlag;
    int flagUnpackPuzzleState;

    SplitPuzzle * puzzleObject;
    MBProgressHUD * _progressHUD;
    APIServerPart * APIObject;
}

@end

@implementation FoldPuzzle
//---------------------------------------------------
- (id) initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {

    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
    }
    return self;
}
//---------------------------------------------------
- (void) viewDidLoad {

    [super viewDidLoad];

    backgroundQueue = dispatch_queue_create("com.Puzzle", NULL);

    splittedImages = [NSMutableArray new];
    centerParts = [NSMutableArray new];
    centerFrames = [NSMutableArray new];
    shuffleImages = [NSMutableArray new];

    NSLog(@"TimeVal = %d", self.time);
    NSLog(@"PCSVal = %d", self.pcs);

    APIObject = [APIServerPart new];
    APIObject.delegate = self;

    [self showProgressHUDWithMessage:@"Creating puzzle"];

    currentTime = self.time;

    isMoved = NO;
    endFold = NO;
    losePuzzle = NO;
    startFlag = 0;

    replayButton.hidden = YES;
    [replayButton.titleLabel setTextColor:[UIColor whiteColor]];

    // get puzzle id
    int puzzleID = [[self.managedObject valueForKey:@"id_image"] integerValue];
    // check puzzle state (completed, new, old)
    if ([[self.managedObject valueForKey:@"completed"] integerValue] == 1) {
        isOldCompleted = YES;
    }

    // check if first puzzle
    if (puzzleID == - 100) {
        isFirstPuzzle = NO;
    }else{
        isFirstPuzzle = YES;
    }
}
//---------------------------------------------------
- (void) viewWillAppear:(BOOL)animated {

    self.navigationController.navigationBarHidden = YES;
    self.tabBarController.tabBar.hidden = YES;

    if (!startFlag) {
        puzzleObject = [SplitPuzzle new];

        // check state:
        // old, completed - restore coordinate
        // new - create new shuffle puzzle

        if ([[self.managedObject valueForKey:@"completed"] integerValue]) {
            flagUnpackPuzzleState = 1;

            // restore coordinates for pieces
            [self unpackObject];

        }else{
            flagUnpackPuzzleState = 0;
            // create new puzzle for current image and frame sizew
            puzzleObject.originalImage = [ResizeImageClass imageByScalingAndCroppingForSize: self.gridImageView.frame.size : self.puzzleImage];
        }

         puzzleObject.viewSize = self.gridImageView.frame.size;

        // create pieces on view
        [self performSelector:@selector(setUpPuzzlePeaceImages) withObject:nil afterDelay:0.5];
        // start timer
        myTimer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(updateTime) userInfo:nil repeats:YES];
        startFlag = 1;
    }

    [[UIApplication sharedApplication] setStatusBarHidden:YES];

    timeLabel.text = [self transformTime];
}
//---------------------------------------------------
- (void) didReceiveMemoryWarning {

    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}
//---------------------------------------------------
- (IBAction) backAction:(id)sender {

    // check puzzle lose
    if (losePuzzle) {
        // remove and clear all data
        [self clearAndCloseController];
        return;
    }

    // save puzzle state - if not completed
    if ([[self.managedObject valueForKey:@"completed"] integerValue]!= 1){

        actionState = 1;
        // calculate frames pieces coords
        [self getPuzzleViewFrames];

    }else{
        [self clearAndCloseController];
    }
}
//---------------------------------------------------
- (void) goToNextController:(id)result {

    [self hideProgressHUD:YES];

    if (actionState) {
        [self clearAndCloseController];
    }
}
//---------------------------------------------------
// clear all
//---------------------------------------------------
- (void) clearAndCloseController {

    // remove split image resources
    [self removeSplittesImages];
    // remove additional layers
    [self clearGraph];

    // stop timer
    if (myTimer) {
        [myTimer invalidate];
    }

    self.navigationController.navigationBarHidden = NO;
    self.tabBarController.tabBar.hidden = NO;

    [self.navigationController popViewControllerAnimated:NO];

    // update tableview in previous controller
    [[NSNotificationCenter defaultCenter] postNotificationName:@"UpdatePuzzleTableCell" object:self];
}
//---------------------------------------------------
#pragma mark inappropriate
//---------------------------------------------------
- (IBAction) inappropriateAction:(id)sender {

    UIActionSheet *actionSheet = [[UIActionSheet alloc] initWithTitle:nil delegate:self cancelButtonTitle:@"Cancel" destructiveButtonTitle:nil otherButtonTitles:@"Report Inappropriate", nil];

    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad){
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Report Inappropriate" message:nil delegate:self cancelButtonTitle:@"Cancel" otherButtonTitles:@"OK", nil];
        alert.tag = 321;
        [alert show];
    }
    else{
        [actionSheet showInView:self.view];
    }
}
//---------------------------------------------------
- (void) actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {

    switch (buttonIndex) {
        case 0:
            [self reportToServer];
            break;
        default:
            break;
    }
}
//---------------------------------------------------
- (void) reportToServer {

    [APIObject setPuzzleInapropriate:[NSString stringWithFormat:@"%d", self.imageID]];
}
//---------------------------------------------------
#pragma mark Setup Puzzle Pieces
//---------------------------------------------------
- (void) setUpPuzzlePeaceImages {

    int hCount;
    int wCount;

    switch (self.pcs) {
        case 1:
            hCount = 2;
            wCount = 2;

            break;
        case 2:
            hCount = 3;
            wCount = 3;
            break;
        case 3:
            hCount = 4;
            wCount = 4;
            break;
        case 4:
            hCount = 5;
            wCount = 5;
            break;

        default:
            return;
            break;
    }

    //---------------------------------------------
    // check - devices
    //---------------------------------------------
       if (!flagUnpackPuzzleState) {
           [puzzleObject splitImageToPuzzleByHCount:hCount pieceWCount:wCount];
       }

       dispatch_sync(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{

           if (!flagUnpackPuzzleState) {
               [puzzleObject setUpPeaceBezierPaths:0];
           }

        // create coordinates pieces array
        [self createCenterCoordinatesArray];

        float mXAddableVal = 0;
        float mYAddableVal = 0;

        for(int i = 0; i < [puzzleObject.pieceBezierPathsMutArray count]; i++) {

            CGRect mCropFrame = [[[puzzleObject.pieceCoordinateRectArray objectAtIndex:i] objectAtIndex:0] CGRectValue];
            CGRect mImageFrame = [[[puzzleObject.pieceCoordinateRectArray objectAtIndex:i] objectAtIndex:1] CGRectValue];

            //--- puzzle peace image.
            UIImageView *mPeace = [UIImageView new];

            // for shaffle
            [mPeace setFrame:CGRectMake(self.view.frame.size.width/2, self.view.frame.size.height/2, mImageFrame.size.width, mImageFrame.size.height)];

            [mPeace setTag:i+100];
            [mPeace setUserInteractionEnabled:YES];
            [mPeace setContentMode:UIViewContentModeScaleAspectFit];

            UIPanGestureRecognizer *panGesture = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(panAnim:)];
            [mPeace addGestureRecognizer:panGesture];

            // addable value
            mXAddableVal = ([[[puzzleObject.pieceTypeValueArray objectAtIndex:i] objectAtIndex:0] intValue] == 1)? puzzleObject.deepnessV:0;
            mYAddableVal = ([[[puzzleObject.pieceTypeValueArray objectAtIndex:i] objectAtIndex:3] intValue] == 1)? puzzleObject.deepnessH:0;

            mCropFrame.origin.x += mXAddableVal;
            mCropFrame.origin.y += mYAddableVal;

            // crop
            [mPeace setImage:[self cropImage: puzzleObject.originalImage
                                    withRect: mCropFrame]];
            // clip
            [self setClippingPath:[puzzleObject.pieceBezierPathsMutArray objectAtIndex:i]:mPeace];

            // -------------------------------------------------
            if (!flagUnpackPuzzleState) {
                CGFloat rotation = [self getRandomRotateValue];
                [puzzleObject.pieceRotationValuesArray replaceObjectAtIndex:i withObject:[NSNumber numberWithFloat:rotation]];
            }

            [mPeace setTransform:CGAffineTransformMakeRotation([[puzzleObject.pieceRotationValuesArray objectAtIndex:i] floatValue])];
            // -------------------------------------------------
            // border line
            CAShapeLayer *mBorderPathLayer = [CAShapeLayer layer];

            [mBorderPathLayer setPath:[[puzzleObject.pieceBezierPathsMutArray objectAtIndex:i] CGPath]];
            [mBorderPathLayer setLineWidth:1.5];
            [mBorderPathLayer setFrame:CGRectZero];
            [mBorderPathLayer setStrokeColor:[UIColor blackColor].CGColor];
            [mBorderPathLayer setFillColor:[UIColor clearColor].CGColor];
            [[mPeace layer] addSublayer:mBorderPathLayer];

            //NSLog(@"path == %@", mBorderPathLayer.path);

            //--- secret border line for touch recognition
            CAShapeLayer *mSecretBorder = [CAShapeLayer layer];
            [mSecretBorder setPath:[[puzzleObject.pieceBezierPathsWithoutHolesMutArray objectAtIndex:i] CGPath]];
            [mSecretBorder setFillColor:[UIColor clearColor].CGColor];
            [mSecretBorder setStrokeColor:[UIColor blackColor].CGColor];
            [mSecretBorder setLineWidth:0];

            [mSecretBorder setFrame:CGRectZero];

            [[mPeace layer] addSublayer:mSecretBorder];

            // add part to array
            [splittedImages addObject:mPeace];

            // add gestures
            UIPanGestureRecognizer *panRecognizer = [[UIPanGestureRecognizer alloc]
                                                     initWithTarget:self action:@selector(move:)];

            [panRecognizer setMinimumNumberOfTouches:1];
            [panRecognizer setMaximumNumberOfTouches:2];
            [panRecognizer setDelegate:(id)self];

            [mPeace addGestureRecognizer:panRecognizer];
        }

        dispatch_async(dispatch_get_main_queue(), ^{

            //*************************
            //---------------------------------------------
            // add splitted part to view
            //---------------------------------------------
            if (flagUnpackPuzzleState) {

                for (int i = 0; i < splittedImages.count; i++) {

                    UIImageView * tmpView = [splittedImages objectAtIndex:i];
                    CGRect myRect = CGRectFromString([arrayPositionsSavedState objectAtIndex:i]);
                    // NSLog(@"myRect = %@", NSStringFromCGRect(myRect));
                    tmpView.frame = myRect;
                    [subView addSubview:tmpView];

                }
            }else {
                shuffleImages = [NSMutableArray arrayWithArray:splittedImages];
                [self shuffleArray];
            }
            //---------------------------------------------

            if (!flagUnpackPuzzleState) {
                [puzzleObject setUpPeaceBezierPaths:1];
            }else if (puzzleObject.newDeviceFlag){
                [puzzleObject setUpPeaceBezierPaths:1];
            }

            [self drawGrid];

            if ([[self.managedObject valueForKey:@"completed"] integerValue] == 1) {
                [self winCondition];

            }

            [self hideProgressHUD:YES];
        });
   });
}
//---------------------------------------------------
#pragma mark Shuffle puzzle pieces
//---------------------------------------------------
- (void) shuffleArray {

    NSUInteger count = [shuffleImages count];

    for (NSUInteger i = 0; i < count; ++i) {
        // Select a random element between i and end of array to swap with.
        NSInteger nElements = count - i;
        NSInteger n = arc4random_uniform(nElements) + i;
        [shuffleImages exchangeObjectAtIndex:i withObjectAtIndex:n];
    }

    float widthOffset = 10;

    for (UIImageView * tmpView  in shuffleImages) {

        tmpView.frame = CGRectMake(widthOffset, self.view.frame.size.height - 220, tmpView.frame.size.width, tmpView.frame.size.height);
        widthOffset +=8;
        [subView addSubview:tmpView];
    }

}
#pragma mark Save state puzzle
//---------------------------------------------------
// for saving
//---------------------------------------------------
- (void) getPuzzleViewFrames {

    NSMutableArray * puzzleFrames = [NSMutableArray new];

    for (int i = 0; i<splittedImages.count; i++) {
        // get all imageview
        UIImageView * tmpView = (UIImageView*)[self.view viewWithTag:i+100];
        [puzzleFrames addObject:[NSString stringWithFormat:@"%@", NSStringFromCGRect(tmpView.frame)]];
    }

    NSData * myData = [NSKeyedArchiver archivedDataWithRootObject:puzzleFrames];
    NSData * myObjectData = [NSKeyedArchiver archivedDataWithRootObject:puzzleObject];

    // actual coord pieces
    [self.managedObject setValue:myData forKey:@"coordinatesArray"];
    // pazzle object
    [self.managedObject setValue:myObjectData forKey:@"puzzleObject"];

    if ([[self.managedObject valueForKey:@"completed"] integerValue]!= 1) {
        // left seconds
        [self.managedObject setValue:@(currentTime) forKey:@"timeLeft"];
        // state - 2 - in progress
        [self.managedObject setValue:@(2) forKey:@"completed"];
    }

    [GBKDB save];

    // save to server status

    [self showProgressHUDWithMessage:@"Saving puzzle state"];

    [myTimer invalidate];

    dispatch_async(backgroundQueue, ^(void) {

        [APIObject setStatusForPuzzle:self.imageID :[[self.managedObject valueForKey:@"completed"] integerValue] :puzzleFrames : puzzleObject : currentTime];
    });
}
//---------------------------------------------------
#pragma mark Restore puzzle state
//---------------------------------------------------
- (void) unpackObject {

    arrayPositionsSavedState = [NSMutableArray new];

	NSData * myData = [self.managedObject valueForKey:@"coordinatesArray"];
    NSData * myDataObject = [self.managedObject valueForKey:@"puzzleObject"];
    currentTime = [[self.managedObject valueForKey:@"timeLeft"] integerValue];

    arrayPositionsSavedState = [NSKeyedUnarchiver unarchiveObjectWithData:myData];
    puzzleObject = [NSKeyedUnarchiver unarchiveObjectWithData:myDataObject];

    // device type
    NSString * dt;
    if (IDIOM == IPAD) {
        dt = @"IPAD";
    }else{
        dt = @"IPHONE";
    }

    if (puzzleObject.deviceType == nil || ![puzzleObject.deviceType isEqualToString:dt])  {
        flagUnpackPuzzleState = 0;
        puzzleObject.originalImage = [UIImage new];
        puzzleObject.originalImage = [ResizeImageClass imageByScalingAndCroppingForSize: self.gridImageView.frame.size : self.puzzleImage];
        currentTime = self.time;
        puzzleObject.pieceBezierPathsMutArray = [NSMutableArray new];
        puzzleObject.pieceBezierPathsWithoutHolesMutArray = [NSMutableArray new];
        puzzleObject.pieceCoordinateRectArray = [NSMutableArray new];
        puzzleObject.pieceTypeValueArray = [NSMutableArray new];
        puzzleObject.pieceRotationValuesArray = [NSMutableArray new];
        arrayPositionsSavedState = [NSMutableArray new];
        [self.managedObject setValue:@(0) forKey:@"completed"];
    }
}
//---------------------------------------------------
#pragma mark Random - Rotate pieces
//---------------------------------------------------
- (CGFloat) getRandomRotateValue {

    CGFloat val;
    int i = arc4random()%4;

    switch (i) {
        case 0:
            val = 0;
            break;
        case 1:
            val = M_PI/2;
            break;
        case 2:
            val = M_PI;
            break;
        case 3:
            val = M_PI + M_PI/2;
            break;
    }

    return val;
}
//---------------------------------------------------
#pragma mark Help functions
//---------------------------------------------------
#pragma mark -- Draw grid in subview layer
//---------------------------------------------------
- (void) drawGrid {

    for(int i = 0; i < [puzzleObject.myPieceBezierPathsMutArray count]; i++)
    {
        //--- border line
        CAShapeLayer *mBorderPathLayer = [CAShapeLayer layer];

        [mBorderPathLayer setPath:[[puzzleObject.myPieceBezierPathsMutArray objectAtIndex:i] CGPath]];
        [mBorderPathLayer setLineWidth:0.5];
        [mBorderPathLayer setStrokeColor:[UIColor blackColor].CGColor];
        [mBorderPathLayer setFillColor:[UIColor clearColor].CGColor];

        [[self.gridImageView layer] addSublayer:mBorderPathLayer];

        [gridLayer addObject:mBorderPathLayer];
    }
}
//---------------------------------------------------
#pragma mark -- Clip
//---------------------------------------------------
- (void) setClippingPath:(UIBezierPath *)clippingPath : (UIImageView *)imgView {

    if (![[imgView layer] mask]) {

        [[imgView layer] setMask:[CAShapeLayer layer]];
    }

    [(CAShapeLayer*) [[imgView layer] mask] setPath:[clippingPath CGPath]];
    [(CAShapeLayer*) [[self.gridImageView layer] mask] setPath:[clippingPath CGPath]];
}
//---------------------------------------------------
#pragma mark -- Crop
//---------------------------------------------------
- (UIImage *) cropImage:(UIImage*)originalImage withRect:(CGRect)rect {

    return [UIImage imageWithCGImage:CGImageCreateWithImageInRect([originalImage CGImage], rect)];
}
//---------------------------------------------------
#pragma mark -- Gester move
//---------------------------------------------------
- (void) move:(id)sender {

    CGPoint translatedPoint = [(UIPanGestureRecognizer*)sender translationInView: subView];

    if(touchedImgViewTag_ == 0 || touchedImgViewTag_ == 99) {

        return;
    }

    UIImageView *mImgView = (UIImageView *)[subView viewWithTag:touchedImgViewTag_];

    translatedPoint = CGPointMake(firstX_+translatedPoint.x, firstY_+translatedPoint.y);

    [mImgView setCenter:translatedPoint];
    //--------------------------------------------------------
    // check border
    //--------------------------------------------------------
    [self setBorderForView:mImgView :mImgView.frame];
}
//---------------------------------------------------
#pragma mark -- Check border
//---------------------------------------------------
- (void) setBorderForView:(UIImageView*) _imageView :(CGRect)objectFrame {

    // Bottom
    if (objectFrame.origin.y > self.view.frame.size.height - _imageView.frame.size.height) {

        _imageView.frame = CGRectMake(_imageView.frame.origin.x, self.view.frame.size.height - _imageView.frame.size.height, _imageView.frame.size.width, _imageView.frame.size.height);
    }
    // Top
    if (objectFrame.origin.y < 0) {

        _imageView.frame = CGRectMake(_imageView.frame.origin.x, 0, _imageView.frame.size.width, _imageView.frame.size.height);
    }
    // left
    if (objectFrame.origin.x < 0) {

        _imageView.frame = CGRectMake(0, _imageView.frame.origin.y, _imageView.frame.size.width, _imageView.frame.size.height);
    }
    // right

    if (objectFrame.origin.x > self.view.frame.size.width - _imageView.frame.size.width) {

        _imageView.frame = CGRectMake(self.view.frame.size.width - _imageView.frame.size.width, _imageView.frame.origin.y, _imageView.frame.size.width, _imageView.frame.size.height);
    }
}
//---------------------------------------------------
#pragma mark -- Gester Recognizers
//---------------------------------------------------
- (BOOL) gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {

	return YES;
}
//---------------------------------------------------
- (void) touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {

    if(touchedImgViewTag_ == 0 || endFold) {

        return;
    }

    UIImageView *mImgView = (UIImageView *)[subView viewWithTag:touchedImgViewTag_];

    if(!mImgView || ![mImgView isKindOfClass:[UIImageView class]]){

        return;
    }

    CGFloat mRotation = [[puzzleObject.pieceRotationValuesArray objectAtIndex:mImgView.tag-100] floatValue];
    [UIView beginAnimations:nil context:nil];
    [UIView setAnimationDuration:0.25];
    [UIView setAnimationCurve:UIViewAnimationCurveEaseInOut];

    if(mRotation >= 0  && mRotation < M_PI/2) {

        [mImgView setTransform:CGAffineTransformMakeRotation(M_PI/2)];
        mRotation = M_PI/2;
    }
    else if(mRotation >= M_PI/2 && mRotation < M_PI) {

        [mImgView setTransform:CGAffineTransformMakeRotation(M_PI)];
        mRotation = M_PI;
    }
    else if(mRotation >= M_PI && mRotation < M_PI + M_PI/2) {

        [mImgView setTransform:CGAffineTransformMakeRotation(M_PI + M_PI/2)];
        mRotation = M_PI + M_PI/2;
    }
    else {

        [mImgView setTransform:CGAffineTransformMakeRotation(M_PI*2)];
        mRotation = 0;
    }

    [UIView commitAnimations];
    [puzzleObject.pieceRotationValuesArray replaceObjectAtIndex:mImgView.tag-100 withObject:[NSNumber numberWithFloat:mRotation]];

    //----------------------------------
    // check roration
    //----------------------------------

    CGFloat realRotation = 0;
     realRotation = [[puzzleObject.pieceRotationValuesArray objectAtIndex: mImgView.tag-100 ] floatValue];

     if (realRotation) {
         return;
     }

    [self checkCollide];
}
//---------------------------------------------------
- (void) touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {

    if (endFold) {

        return;
    }

    for (UIImageView * tmpView  in splittedImages) {

        tmpView.layer.zPosition = 0;
    }

    touchedImgViewTag_ = 0;
    UIImageView *mImgView = nil;
    UITouch *touch = [[event allTouches] anyObject];
    CGPoint location = [touch locationInView:subView];

    for(int i = [[subView subviews] count]-1; i > -1 ; i--) {

        mImgView = (UIImageView *)[[subView subviews] objectAtIndex:i];
        mImgView.layer.zPosition = 1;
        location = [touch locationInView:mImgView];

        if(CGPathContainsPoint([(CAShapeLayer*) [[[mImgView layer] sublayers] objectAtIndex:1] path], nil, location, NO)) {

            touchedImgViewTag_ = mImgView.tag;
            [subView bringSubviewToFront:mImgView];

            firstX_ = mImgView.center.x;
            firstY_ = mImgView.center.y;

            break;
        }
    }
}
//---------------------------------------------------
- (UIImageView *)imageFromArray:(NSInteger)tag {

    for (UIImageView *image in splittedImages) {

        if (image.tag == tag)

            return image;
    }
    return nil;
}
//---------------------------------------------------
- (void) panAnim:(UIPanGestureRecognizer *) gestureRecognizer {

    if(gestureRecognizer.state == UIGestureRecognizerStateEnded) {

        for (UIImageView * tmpView  in splittedImages) {

            tmpView.layer.zPosition = 0;
        }

        if (!touchedImgViewTag_) {

            return;
        }

        UIImageView *mImgView = [self imageFromArray:touchedImgViewTag_];
        NSLog(@"mImgView %@", mImgView);
        CGPoint newCenter = CGPointFromString([centerParts objectAtIndex:touchedImgViewTag_-100]);
        NSLog(@"new center %@", NSStringFromCGPoint(newCenter));
        CGRect frameOffsetPos = CGRectFromString([centerFrames objectAtIndex:touchedImgViewTag_-100]);
        NSLog(@"frameOffsetPos %@", NSStringFromCGRect(frameOffsetPos));

        CGFloat realRotation = 0;
        //----------------------------------
        // check rotatiton
        //----------------------------------
        realRotation = [[puzzleObject.pieceRotationValuesArray objectAtIndex: mImgView.tag-100 ] floatValue];

        if (realRotation) {

            return;
        }
        //----------------------------------

        if (CGRectContainsPoint(frameOffsetPos, mImgView.center)) {

            [UIView animateWithDuration:0.1 animations:^{

                [mImgView setCenter:newCenter];

            } completion:^(BOOL finished) {

                [self checkCollide];
            }];
        }
    }
}
//---------------------------------------------------
#pragma mark -- Create array of centers
//---------------------------------------------------
- (void) createCenterCoordinatesArray {

    CGRect frameOriginal;

    float distanceX = 0;
    float distanceY = 0;
    float offsetX = 0;
    float offsetY = 0;
    float offsetViewX = foldPuzzleView.frame.origin.x; // left offset
    float offsetViewY = foldPuzzleView.frame.origin.y; // top offset

    if (IDIOM == IPAD) {

        offsetViewX += 1;
        offsetViewY += 1;
    }

    float rectOffset = 80;

    for (int i = 0; i < [puzzleObject.pieceBezierPathsMutArray count]; i++) {

        frameOriginal = [[[puzzleObject.pieceCoordinateRectArray objectAtIndex:i] objectAtIndex:1] CGRectValue];

        offsetX = frameOriginal.origin.x;
        offsetY = frameOriginal.origin.y;

        distanceX = frameOriginal.size.width/2 + offsetX + offsetViewX;
        distanceY = frameOriginal.size.height/2 + offsetY + offsetViewY;

        CGPoint disPoint = CGPointMake(distanceX, distanceY);
        CGRect newFrame = CGRectMake(disPoint.x-rectOffset/2, disPoint.y-rectOffset/2, rectOffset, rectOffset);

        [centerParts addObject: NSStringFromCGPoint(disPoint)];
        [centerFrames addObject:NSStringFromCGRect(newFrame)];
    }
}
//---------------------------------------------------
#pragma mark -- Remove split images
//---------------------------------------------------
- (void) removeSplittesImages {

    if (!splittedImages) {

        splittedImages = [NSMutableArray new];
    }
    else{

        for (UIImageView * part in splittedImages) {

            [part removeFromSuperview];
        }

        splittedImages = [NSMutableArray new];
    }
}
//---------------------------------------------------
#pragma mark -- Clear grid
//---------------------------------------------------
- (void) clearGraph {

    for (CALayer *layer in gridLayer) {

        [layer removeFromSuperlayer];
    }

    splittedImages = [NSMutableArray new];
}
//---------------------------------------------------
#pragma mark -- Collide with pieces
//---------------------------------------------------
- (void) checkCollide {

    NSMutableArray * checks = [NSMutableArray new];

    for (int i = 0; i < [puzzleObject.pieceBezierPathsMutArray count]; i++) {

        CGRect rectCurrentPos = CGRectFromString([centerFrames objectAtIndex:i]);
        CGPoint newCenter = CGPointFromString([centerParts objectAtIndex:i]);
        UIImageView * tmpImg = [splittedImages objectAtIndex:i];

        if (CGRectContainsPoint(rectCurrentPos, tmpImg.center)) {

            [tmpImg setCenter:newCenter];
            [checks addObject:@"1"];
        }else{

            [checks addObject:@"0"];
        }
    }

    int accepted = 0;

    for (int i = 0; i < [puzzleObject.pieceBezierPathsMutArray count]; i++) {

        NSString * val = [checks objectAtIndex:i];
        if (![val isEqualToString:@"1"]) {

            break;
        }else{

            accepted++;
        }
    }

    if (accepted == [puzzleObject.pieceBezierPathsMutArray count]) {

        [self.managedObject setValue:@(1) forKey:@"completed"];
        [GBKDB save];

        [self winCondition];
    }
}
//---------------------------------------------------
#pragma mark -- Check Puzzle completed
//---------------------------------------------------
- (void) winCondition {

    resultView.hidden = NO;

    NSString * strSepia = [NSString stringWithFormat:@"%d",0x82D482];
    UIColor * colorSepia = [HexColor colorFromHex:[strSepia intValue]];

    [resultLabel setTextColor:colorSepia];

    // remove all gesters and set view userentaraction NO
    [self blockPuzzle];
    endFold = YES;

    [myTimer invalidate];

    // if puzzle is old -
    // skip saving state and call api
    if (!isOldCompleted) {
        [self getPuzzleViewFrames];
    }

    if ([AppSettings isFriendExist] && isFirstPuzzle) {
        replayButton.hidden = NO;
    }else{
        replayButton.hidden = YES;
    }

}
//---------------------------------------------------
#pragma mark -- Check Collide
//---------------------------------------------------
- (BOOL) viewsDoCollide:(CGRect)frame1 :(CGRect)frame2 {

    if(CGRectIntersectsRect(frame1, frame2)) {

        return YES;
    }

    return NO;
}
//---------------------------------------------------
#pragma mark -- Update Timer
//---------------------------------------------------
- (void) updateTime {

    currentTime--;

    if (currentTime <=0) {

        [myTimer invalidate];

        resultView.hidden = NO;
        NSString * strSepia = [NSString stringWithFormat:@"%d",0xe2876c];
        UIColor * colorSepia = [HexColor colorFromHex:[strSepia intValue]];

        [resultLabel setTextColor:colorSepia];
         resultLabel.text = @"Time is Up!!!";
        endFold = YES;
        losePuzzle = YES;
        [self blockPuzzle];

        // remove
        [APIObject removePuzzle:[NSString stringWithFormat:@"%d", self.imageID]];
        [GBKDB delete:(GBKManagedObject*)self.managedObject];
        [GBKDB save];
    }

    timeLabel.text = [self transformTime];
}
//---------------------------------------------------
#pragma mark -- Lock Puzzle
//---------------------------------------------------
- (void) blockPuzzle {

    if ([AppSettings isFriendExist]) {

        replayButton.hidden = NO;
    }

    for (UIImageView *image in splittedImages) {

        [image setUserInteractionEnabled:NO];
    }
}
//---------------------------------------------------
#pragma mark -- Time Label Format
//---------------------------------------------------
- (NSString *) transformTime {

    int firstPart = currentTime / 60;
    int secondPart = currentTime - firstPart*60;
    NSString * first;
    NSString * second;

    if (currentTime < 60) {

        firstPart = 0;
        secondPart = currentTime;
    }

    if (firstPart < 10) {

         first = [NSString stringWithFormat:@"0%d", firstPart];
    }else{

         first = [NSString stringWithFormat:@"%d", firstPart];
    }

    if (secondPart < 10) {

        second = [NSString stringWithFormat:@"0%d", secondPart];
    }else{

        second = [NSString stringWithFormat:@"%d", secondPart];
    }

    NSString * createTime = [NSString stringWithFormat:@"%@:%@", first, second];

    return createTime;
}
//---------------------------------------------------
#pragma mark -- Alert and HUD
//---------------------------------------------------
- (void) alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{

    if (alertView.tag == 321) {

        if (buttonIndex == 1) {

            [self reportToServer];
        }
    }
    else{

        NSString * buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
        if([buttonTitle isEqualToString:@"YES"]) {

            [self backAction:nil];
        }
    }
}
//---------------------------------------------------
- (MBProgressHUD *) progressHUD {

    if (!_progressHUD) {

        _progressHUD = [[MBProgressHUD alloc] initWithView:self.view];
        _progressHUD.minSize = CGSizeMake(120, 120);
        _progressHUD.minShowTime = 1;
        self.progressHUD.customView = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"MWPhotoBrowser.bundle/images/Checkmark.png"]];
        [self.view addSubview:_progressHUD];
    }

    return _progressHUD;
}
//---------------------------------------------------
- (void) showProgressHUDWithMessage:(NSString *)message {

    self.progressHUD.labelText = message;
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:YES];
    self.navigationController.navigationBar.userInteractionEnabled = NO;
}
//---------------------------------------------------
- (void) hideProgressHUD:(BOOL)animated {

    [self.progressHUD hide:animated];
    self.navigationController.navigationBar.userInteractionEnabled = YES;
}
//---------------------------------------------------
- (IBAction) replayAction:(id)sender {

    TakePhotoPuzzle * takePhotoViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"TakePhotoPuzzle"];
    takePhotoViewController.callFlag = 2;

    [AppSettings setSendTitle:@"RESEND"];
    [self.navigationController pushViewController:takePhotoViewController animated:NO];
}
//---------------------------------------------------
@end
