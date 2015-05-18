//
//  Controller.swift
//
//  Copyright (c) 2011-2015 gbksoft. All rights reserved.
//

import Foundation
import UIKit

//********************************************
// MARK: -- COLLECTION CELL POST
//********************************************

class CustomPhotoCollectionCell: UICollectionViewCell {

    @IBOutlet var photoView     : UIImageView!
    @IBOutlet var videoIconView : UIView!
    
    var post_id         : String!
    var reposteduser_id : String?
}
//********************************************
// MARK: -- COLLECTION CELL FOLLOW
//********************************************
class CustomFollowCollectionCell: UICollectionViewCell {
    @IBOutlet var avatarImage   : UIImageView!
    @IBOutlet var usernameLabel : UILabel!
    @IBOutlet var actionButton  : UIButton!
    
    var id_user : String!
    
    var collectionDelegate : ProfileViewController!
    
    //--------------------------------------------
    override func awakeFromNib() {
        
        usernameLabel.font = UIFont (name: "Myriad Web Pro", size: 18)
        usernameLabel.textColor = UIColor(red: 84.0/255, green: 93.0/255, blue: 98.0/255, alpha: 1.0)
        
        avatarImage.layer.cornerRadius = avatarImage.frame.size.width/2
    }
    //--------------------------------------------
    @IBAction func followAction(sender: AnyObject) {
        
        collectionDelegate.callFollowUserAPI(id_user, success: { (value) -> Void in
            
            self.setFollowButtonState(value.is_following)
        })
    }
    //--------------------------------------------
    func setFollowButtonState(state: Int32) {
        
        if state == 0 {
            
            actionButton.setBackgroundImage(UIImage(named: "adduser_notif"), forState: UIControlState.Normal)
            
        }else{
            
            actionButton.setBackgroundImage(UIImage(named: "checkbox_on"), forState: UIControlState.Normal)
        }
    }
}
//********************************************
// MARK: -- COLLECTION HEADER
//********************************************
class CustomHeaderCollectionView : UICollectionReusableView {
    
    @IBOutlet var copylinkView          : UIView!
    
    @IBOutlet var userNameLabel         : UILabel!
    @IBOutlet var postCountLabel        : UILabel!
    @IBOutlet var postLabel             : UILabel!
    @IBOutlet var followersCountLabel   : UILabel!
    @IBOutlet var followersLabel        : UILabel!
    @IBOutlet var followingsCountLabel  : UILabel!
    @IBOutlet var followingLabel        : UILabel!
    @IBOutlet var bioLabel              : UILabel!
    @IBOutlet var websiteLabel          : UILabel!
    
    @IBOutlet var followButton          : UIButton!
    @IBOutlet var headerButton          : UIButton!
    
    @IBOutlet var pageImageView         : UIImageView!
    @IBOutlet var headerImage           : UIImageView!
    @IBOutlet var okButton              : UIImageView!
    
    @IBOutlet var scrollView            : UIScrollView!
    
    @IBOutlet var postsView             : UIView!
    @IBOutlet var followerView          : UIView!
    @IBOutlet var followingView         : UIView!
    

//--------------------------------------------
    func didReceiveURL(url: NSURL) {
        
        var image: UIImage?
        var request: NSURLRequest = NSURLRequest(URL: url)
        
        NSURLConnection.sendAsynchronousRequest(request, queue: NSOperationQueue.mainQueue(), completionHandler: {(response: NSURLResponse!, data: NSData!, error: NSError!) -> Void in
            
            image = UIImage(data: data)
            self.headerImage.image = image
            self.headerButton.setBackgroundImage(UIImage(), forState: UIControlState.Normal)
        })
    }
}
//********************************************
// MARK: -- CONTROLLER
//********************************************
class ProfileViewController: CoreViewController, UICollectionViewDataSource, UICollectionViewDelegate, UIActionSheetDelegate, UIScrollViewDelegate {
    
    @IBOutlet var profileCollectionView : UICollectionView!
    
    var showFlag                        : Int!
    var reusableView                    : CustomHeaderCollectionView!
    var scrollContentSize               : CGSize?
    var postCellSize                    : CGSize?
    var userCellSize                    : CGSize?
    
    var postsData    : NSMutableArray = []
    var followData   : NSMutableArray = []
    
    var limitPostsOnRequest : Int32 = 16
    var pageCount           : Int32 = 0

    var userData            : UserClass?
    
    var block               = 0
    var callFromMenu        = 0
    var refreshControl      = UIRefreshControl()

    var isRefresh = false
    
//--------------------------------------------
// Apply view custom design
//--------------------------------------------
override func applyDesing() {
    
    var height = self.navigationController?.navigationBar.frame.height
    var offset = self.navigationController?.navigationBar.frame.origin.y
    height! += offset!
    
    var widht  = self.navigationController?.view.frame.size.width
    widht! /= 2
    
    var x   = widht! - widht!/2
    var y   = self.navigationController?.view.frame.origin.y        
    
    var view : UIView = UIView(frame: CGRectMake(x, y!, widht!, height!))
    
    var tap = UITapGestureRecognizer(target: self, action:Selector("showAction:"))
    view.addGestureRecognizer(tap)
    
    self.navigationController?.view.addSubview(view)
    
    if (callFromMenu == 0) {
        
        leftButton.setBackgroundImage(UIImage(named: "back_arrow"), forState: UIControlState.Normal)
        leftButton.frame = CGRect(x: 0, y: 0, width: 14, height: 14)
        rightButton.enabled = false
        rightButton.hidden = true
    }
    
    showFlag = 0
    
    refreshControl.addTarget(self, action: Selector("startRefresh:"), forControlEvents: UIControlEvents.ValueChanged)
    profileCollectionView.addSubview(refreshControl)
    profileCollectionView.alwaysBounceVertical = true
}
//--------------------------------------------
    override func viewWillAppear(animated: Bool) {
        
        self.navigationItem.title = userData!.username
        
        // Get all post for current user
        callPostAPI { (value) -> Void in}
    }
//--------------------------------------------
// Left action nav button
//--------------------------------------------
    override func leftAction(sender: AnyObject) {
        
        if (callFromMenu == 0){
            
            navigationController?.popViewControllerAnimated(true)
        }else{
            
            callMenuAction()
        }
    }
//--------------------------------------------
// Set actions
//--------------------------------------------
    func reportAction(option:String, value: String) {
        
        var reportObject = ReportClass()
        
        reportObject.type_id       = "3"
        reportObject.option_id     = option
        reportObject.reported_id   = value
        
        if (option == "1") {
            
            var tmpValue = userData?.is_blocked
            
            RestAPIClass.blockUser(delegate.userData.access_token, user_id: userData?.id_user, success: { (response) -> Void in
                
                if (tmpValue == "0") {
                    
                    self.showAlert("", message: "User blocked")
                    self.userData?.is_blocked = "1"
                }else{
                    
                    self.showAlert("", message: "User unblocked")
                    self.userData?.is_blocked = "0"
                }
                
                }) { (errorMessage) -> Void in
                    
                    self.showAlert("", message: errorMessage as String!)
            }
            
        } else {
            
            RestAPIClass.reportSystem(delegate.userData.access_token, report: reportObject, success: { (response) -> Void in
                
                self.showAlert("", message: "Profile Reported")
                
                }) { (errorMessage) -> Void in
                    
                    self.showAlert("", message: errorMessage as String!)
            }
        }
    }
//--------------------------------------------
// Show actions
//--------------------------------------------
    func showAction(sender: AnyObject){
        
        var actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: UIAlertControllerStyle.ActionSheet)
        
        
        if (userData?.id_user != delegate.userData.id_user){
            
            var title : String!
            
            if userData?.is_blocked == "0"{
                title = "Block"
            }
            else{
                title = "Unblock"
            }
            
            let reportAction  = UIAlertAction(title: title, style: .Destructive) { (action) in
                
                self.showQuestionAlert("1", value: (self.userData?.id_user)!)
            }
            
            let reportAction2 = UIAlertAction(title: "Report Profile", style: .Destructive) { (action) in

                self.showQuestionAlert("2", value: (self.userData?.id_user)!)
            }
            
            actionSheet.addAction(reportAction)
            actionSheet.addAction(reportAction2)
        }
        
        actionSheet.addAction(UIAlertAction(title: "Copy Profile Link", style: UIAlertActionStyle.Default, handler: { (action) -> Void in
            
            self.showCopyLinkForm()
        }))
        
        actionSheet.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Cancel, handler: nil))
        
        self.presentViewController(actionSheet, animated: true, completion: nil)
    }
//--------------------------------------------
// Question AlertView
//--------------------------------------------
    func showQuestionAlert(option:String, value: String){
        
        var alert = UIAlertController(title: "", message: "Are You Sure?", preferredStyle: UIAlertControllerStyle.Alert)

        alert.addAction(UIAlertAction(title: "No", style: UIAlertActionStyle.Cancel, handler: nil))
        alert.addAction(UIAlertAction(title: "Yes", style: UIAlertActionStyle.Default, handler: { (action) -> Void in
            
            self.reportAction(option, value: value)
        }))
        
        self.presentViewController(alert, animated: true, completion: nil)
    }

//--------------------------------------------
// Copy Link From
//--------------------------------------------
    func showCopyLinkForm(){
        
        reusableView.copylinkView.setTranslatesAutoresizingMaskIntoConstraints(true)

        var rect = reusableView.copylinkView.frame
        
        reusableView.copylinkView.frame = rect
        reusableView.copylinkView.hidden = false
        reusableView.copylinkView.backgroundColor = UIColor(red: 141.0/255, green: 72.0/255, blue: 75.0/255, alpha: 1.0)
        
        var postboard = UIPasteboard.generalPasteboard()
        postboard.string = userData?.profile_url

        rect.origin.y += rect.height
        
        UIView.animateWithDuration(0.5, animations: { () -> Void in
            
            self.reusableView.copylinkView.frame = rect

        }) { (flag) -> Void in
            
            var timer = NSTimer.scheduledTimerWithTimeInterval(0.5, target: self, selector: Selector("hideMessageView"), userInfo: nil, repeats: false)
        }
    }
//--------------------------------------------
// Top scroll view
//--------------------------------------------
    func scrollViewDidScroll(scrollView: UIScrollView) {

        if (scrollView == reusableView.scrollView) {
            
            var page = reusableView.scrollView.contentOffset.x/self.view.frame.width
            
            if (page == 0) {
                
                reusableView.pageImageView.image = UIImage(named: "page1.png")
            }else if (page == 1) {
                
                reusableView.pageImageView.image = UIImage(named: "page2.png")
            }
            
        }else{
            
            if block > 0 {
                return
            }
            
            var offset : CGPoint = scrollView.contentOffset
            var bounds : CGRect  = scrollView.bounds
            var size   : CGSize  = scrollView.contentSize
            var inset  : UIEdgeInsets = scrollView.contentInset
            
            var y = offset.y + bounds.size.height - inset.bottom
            var h = size.height
            
            var reload_distance = 10.0 as CGFloat
            
            if (y > h + reload_distance){
                
                block = 1
                var queue = dispatch_queue_create("com.project", nil)
                
                dispatch_async(queue, { () -> Void in
                    
                    self.callPostAPI({ (value) -> Void in})
                })
            }
        }
    }
//--------------------------------------------
    override func viewDidLayoutSubviews() {

        if reusableView != nil{

            reusableView.scrollView.contentSize = scrollContentSize!
        }
    }
//--------------------------------------------
    func hideMessageView() {
        
        reusableView.copylinkView.setTranslatesAutoresizingMaskIntoConstraints(true)
        
        var rect = reusableView.copylinkView.frame
        rect.origin.y -= rect.height
        
        UIView.animateWithDuration(0.5, animations: { () -> Void in
            
            self.reusableView.copylinkView.frame = rect
            
            }) { (flag) -> Void in
                self.reusableView.copylinkView.hidden = true
        }
    }
//--------------------------------------------
// Open url in new controller
//--------------------------------------------
    func openWebView(sender:AnyObject) {
        
        let webViewController = self.storyboard?.instantiateViewControllerWithIdentifier("CustomWebViewController") as! CustomWebViewController
        
        webViewController.mylink = userData!.website
        navigationController?.pushViewController(webViewController, animated: true)
    }
//--------------------------------------------
// MARK: Collection view life circle
//--------------------------------------------
func collectionView(collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, atIndexPath indexPath: NSIndexPath) -> UICollectionReusableView {
    
    if (kind == UICollectionElementKindSectionHeader) {
        
        reusableView = collectionView.dequeueReusableSupplementaryViewOfKind(UICollectionElementKindSectionHeader, withReuseIdentifier: "ProfileCollectionHeader", forIndexPath: indexPath) as! CustomHeaderCollectionView
     
        reusableView.userNameLabel.font = UIFont (name: "Myriad Pro", size: 15)
        reusableView.followButton.titleLabel?.font = UIFont (name: "Myriad Pro", size: 8.8)
        reusableView.postCountLabel.font = UIFont (name: "Myriad Pro", size: 18)
        reusableView.followersCountLabel.font = UIFont (name: "Myriad Pro", size: 18)
        reusableView.followingsCountLabel.font = UIFont (name: "Myriad Pro", size: 18)
        
        reusableView.postLabel.font =  UIFont (name: "Myriad Web Pro", size: 14)
        reusableView.followersLabel.font =  UIFont (name: "Myriad Web Pro", size: 14)
        reusableView.followingLabel.font =  UIFont (name: "Myriad Web Pro", size: 14)
        reusableView.bioLabel.font =  UIFont (name: "Myriad Web Pro", size: 12)
        reusableView.websiteLabel.font =  UIFont (name: "Myriad Web Pro", size: 12)
        

        scrollContentSize = CGSizeMake(self.view.frame.size.width * 2, reusableView.scrollView.frame.size.height)
        self.reusableView.scrollView.contentSize = scrollContentSize!
        self.updateHeaderCollection()
    }
    
    return reusableView
}
//--------------------------------------------
// Update header Collection
//--------------------------------------------
    func updateHeaderCollection() {
        
        reusableView.userNameLabel.text = userData!.username
        reusableView.bioLabel.text = userData!.status_text_description + userData!.country
        reusableView.websiteLabel.text = userData!.website
        
        reusableView.postCountLabel.text = String(userData!.count_posts)
        reusableView.followersCountLabel.text = String(userData!.count_follower)
        reusableView.followingsCountLabel.text = String(userData!.count_following)
        reusableView.websiteLabel.textColor = UIColor(red: 27.0/255, green: 148.0/255, blue: 190.0/255, alpha: 1.0)
        
        // tap on website url
        var tap = UITapGestureRecognizer(target: self, action:Selector("openWebView:"))
        reusableView.websiteLabel.addGestureRecognizer(tap)
        
        // show hide is follow current user
        if (userData?.id_user != delegate.userData.id_user) {
            
            reusableView.followButton.hidden = false
            
            switch (userData?.private_flag)! {
            case 0:
                
                if(userData?.is_following > 0){
                    
                    reusableView.followButton.setTitle("UNFOLLOW", forState: .Normal)
                    reusableView.okButton.hidden = false
                    
                }else{
                    reusableView.followButton.setTitle("FOLLOW", forState: .Normal)
                    reusableView.okButton.hidden = true
                }
            case 1:
                
                if(userData?.is_following > 0){
                    
                    reusableView.followButton.setTitle("UNFOLLOW", forState: .Normal)
                    reusableView.okButton.hidden = false
                    
                }else if (userData?.is_requested?.integerValue == 1){
                    reusableView.followButton.setTitle("REQUESTED", forState: .Normal)
                    reusableView.okButton.hidden = true
                }else{
                    reusableView.followButton.setTitle("FOLLOW", forState: .Normal)
                    reusableView.okButton.hidden = true
                }
                
            default:
                println("wrong private_flag")
            }
        }
        
        if count(userData!.header_url) > 0 {
            
            reusableView.didReceiveURL(NSURL(string: userData!.header_url)!)
        }
        
    }
//--------------------------------------------
// Set height for cells
//--------------------------------------------
    func collectionView(collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAtIndexPath indexPath: NSIndexPath) -> CGSize {
        
        if(showFlag == 0) {

            if (postCellSize == nil) {
                
                postCellSize = CGSizeMake((view.frame.size.width/3)-1, (view.frame.size.width/3)-1)
            }

            return postCellSize!
        }else{
            
            if (userCellSize == nil) {
                
                userCellSize = CGSizeMake(profileCollectionView.frame.size.width, 64)
            }
            
            return userCellSize!
        }
    }
//--------------------------------------------
// Cells count
//--------------------------------------------
func collectionView(collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
    
    if (showFlag == 0) {
        
        return postsData.count
    }else{
        
        return followData.count
    }
}
//--------------------------------------------
// Apply design for cell
//--------------------------------------------
func collectionView(collectionView: UICollectionView, cellForItemAtIndexPath indexPath: NSIndexPath) -> UICollectionViewCell {
    
    if (showFlag == 0) {
        
        var postCell = CustomPhotoCollectionCell()
        postCell = profileCollectionView.dequeueReusableCellWithReuseIdentifier("CustomPhotoCollectionCell", forIndexPath: indexPath) as! CustomPhotoCollectionCell
        
         let post = postsData[indexPath.row] as! PostClass

        postCell.post_id = post.id_post
        postCell.reposteduser_id = post.reposteduser?.id_user
        
        var error : NSError = NSError()
        postCell.videoIconView.hidden = true
        
        
        postCell.photoView.sd_setImageWithURL(NSURL(string: post.thumb_url!), placeholderImage: HexColor.imageWithColor(UIColor.whiteColor(), postCell.photoView.frame)) { (image, error, cashe, image_url) -> Void in
            
            //  show video
            if (post.is_video > 0) {
                postCell.videoIconView.hidden = false
                
            }else{
                postCell.videoIconView.hidden = true
            }
        }
        
        return postCell
        
    }else{
        
        var userCell = CustomFollowCollectionCell()
        
        userCell = profileCollectionView.dequeueReusableCellWithReuseIdentifier("CustomFollowCollectionCell", forIndexPath: indexPath) as! CustomFollowCollectionCell
        userCell.collectionDelegate = self
        
        let user = followData[indexPath.row] as! FollowUserClass
        
        userCell.avatarImage.sd_setImageWithURL(NSURL(string: user.thumb_url), placeholderImage: HexColor.imageWithColor(UIColor.blackColor(), userCell.avatarImage.frame))
        
        userCell.usernameLabel.text = user.username
        userCell.id_user = user.id_user
        
        if (user.id_user == delegate.userData.id_user) {
            
            userCell.actionButton.hidden = true
        }else{
            
            userCell.actionButton.hidden = false
        }
        
        if (user.is_following == 0) {
            
            userCell.actionButton.setBackgroundImage(UIImage(named: "adduser_notif"), forState: UIControlState.Normal)
        }else{
            
            userCell.actionButton.setBackgroundImage(UIImage(named: "checkbox_on"), forState: UIControlState.Normal)
        }
        
        userCell.actionButton.tag = Int(user.is_following)
        
        return userCell
    }
}
//--------------------------------------------
// Select row
//--------------------------------------------
    func collectionView(collectionView: UICollectionView, didSelectItemAtIndexPath indexPath: NSIndexPath) {
        
        if (showFlag == 0) {
            
            var photoCell = profileCollectionView.cellForItemAtIndexPath(indexPath) as! CustomPhotoCollectionCell
            
            initActivityView()
            RestAPIClass.getPostByID(delegate.userData.access_token, post_id: photoCell.post_id, repostuser_id:photoCell.reposteduser_id, success: { (result) -> Void in
                
                let postView = self.storyboard?.instantiateViewControllerWithIdentifier("CurrentPostViewController") as! CurrentPostViewController
                postView.currentPost = result
                self.navigationController?.pushViewController(postView, animated: true)
                
                self.removeActivityView()
                
                }) { (errorMessage) -> Void in
                    
                    println(errorMessage)
                    self.removeActivityView()
                    self.showAlert("Information", message: errorMessage)
            }
        }
            
        else{
            
            var userCell = profileCollectionView.cellForItemAtIndexPath(indexPath) as! CustomFollowCollectionCell
            callUserAPI(userCell.id_user, ind: 0, success: { (value) -> Void in})
        }
    }
//--------------------------------------------
// MARK : Header Actions
//--------------------------------------------
// Post
//--------------------------------------------
    @IBAction func postAction(sender:AnyObject) {
    
        showFlag = sender.tag
        startRefresh(nil)
    }
//--------------------------------------------
// Followers
//--------------------------------------------
    @IBAction func followersAction(sender:AnyObject?) {
    
        showFlag = sender?.tag
        callFollowAPI(0)
    }
//--------------------------------------------
// Followings
//--------------------------------------------
    @IBAction func followingsAction(sender:AnyObject?) {
    
        showFlag = sender?.tag
        callFollowAPI(1)
    }
//--------------------------------------------
// User Follow
//--------------------------------------------
    @IBAction func userFollowAction(sender:AnyObject) {
        
        if (reusableView.followButton.titleLabel?.text == "UNFOLLOW") {
            
            var actionSheet = UIAlertController(title: nil, message: nil, preferredStyle: UIAlertControllerStyle.ActionSheet)
            
            let unfollowAction = UIAlertAction(title: "Unfollow", style: .Destructive) { (action) in
                
                self.callFollowCurrentUserAPI()
            }
            
            actionSheet.addAction(unfollowAction)
            actionSheet.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.Cancel, handler: nil))
            
            self.presentViewController(actionSheet, animated: true, completion: nil)
            
        }else {
            
            callFollowCurrentUserAPI()
        }
    }
//--------------------------------------------
// Follow current user
//--------------------------------------------
    func callFollowCurrentUserAPI(){
        
        callFollowUserAPI((userData?.id_user)!, success: { (value) -> Void in
            
            self.userData?.is_following = value.is_following
            
            self.updateHeaderCollection()
        })
    }
//--------------------------------------------
// Get posts
//--------------------------------------------
    func callPostAPI(success:(value: Bool?) -> Void){
        
        var id_user : String!
        
        if (userData?.id_user == delegate.userData.id_user) {
            
            id_user = delegate.userData.id_user
        }else{
            
            id_user = userData?.id_user
        }
        
        RestAPIClass.getPostByUserID(delegate.userData.access_token, id_user:id_user, page:pageCount, limit: limitPostsOnRequest, success: { (response) -> Void in
            
            if (self.isRefresh) {
                self.postsData = NSMutableArray()
                self.isRefresh = false
            }

            self.postsData.addObjectsFromArray(response as [AnyObject]!)
            
            if (response!.count > 0) {
                
                self.profileCollectionView.reloadData()
                self.profileCollectionView.performBatchUpdates(nil, completion: nil)
                // increase counter page
                self.pageCount++                                
            }
            
            // unclock scrollprocess
            self.block = 0
            
            success(value: true)
            
            }, failure: { (errorMessage) -> Void in
                println(errorMessage)
        })
    }
//--------------------------------------------
// Get follower/following user
//--------------------------------------------
    func callFollowAPI(ind : Int) {
        
        initActivityView()
        
        // check for who user call api
        var id_user: String?
        id_user = userData?.id_user
        
        RestAPIClass.getUserFollowUsers(delegate.userData.access_token, id_user: id_user, ind: ind, success: { (contacts) -> Void in
            
            if (contacts!.count > 0) {
                
                self.followData = NSMutableArray(array:(contacts!))                
                
            }else {
                
                if (ind == 0) {
                    
                    self.showAlert("Information", message: "You have no followers")
                }else{
                    
                    self.showAlert("Information", message: "You have no followings")
                }
                
                self.followData = NSMutableArray()
            }
            
            self.profileCollectionView.reloadData()
            self.profileCollectionView.performBatchUpdates(nil, completion: nil)
            
            self.removeActivityView()
            
            self.refreshControl.endRefreshing()
            
        }) { (errorMessage) -> Void in

            println(errorMessage)
            self.removeActivityView()
            self.refreshControl.endRefreshing()
        }
    }
//--------------------------------------------
// Get User data
//--------------------------------------------
    func callUserAPI(id_user:String, ind:Int, success:(value: Bool?) -> Void ){
        
        RestAPIClass.getUserData(delegate.userData.access_token, id_user: id_user, success: { (userData) -> Void in
            
            if(ind == 0) {   // show profile selected user
                
                let controller : ProfileViewController = self.storyboard?.instantiateViewControllerWithIdentifier("ProfileViewController") as! ProfileViewController
            
                controller.userData = userData
                self.navigationController?.pushViewController(controller, animated: true)
                
            }else{          // update current user data
                
                self.userData = userData
                self.updateHeaderCollection()
            }
            
            self.refreshControl.endRefreshing()
            
            success(value: true)
            
            }) { (errorMessage) -> Void in
                
                self.refreshControl.endRefreshing()
                println(errorMessage)
        }
    }
//--------------------------------------------
// Follow/ Unfollow user
//--------------------------------------------
    func callFollowUserAPI(id_user: String, success:(value: FollowUserClass!) -> Void) {
        
        RestAPIClass.addremoveFollower(delegate.userData.access_token, friends_id: id_user, key: "id_user", success: { (follower) -> Void in
           
            success(value: follower)
            
        }) { (errorMessage) -> Void in
            
                self.showAlert("Information", message: errorMessage)
        }
    }
//--------------------------------------------
// Refresh by swipe
//--------------------------------------------
    func startRefresh(sender:AnyObject?) {
        
        pageCount = 0
        isRefresh = true
        if (showFlag > 0) {
            
            self.callFollowAPI(self.showFlag-1)
            
        }else{
            
            callPostAPI { (value) -> Void in
                
                self.callUserAPI((self.userData?.id_user)!, ind: 1, success: { (value) -> Void in
                    
                    self.refreshControl.endRefreshing()
                })
            }
        }
    }
//--------------------------------------------
}
