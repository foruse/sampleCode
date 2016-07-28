import Foundation
import UIKit
import PromiseKit
import CoreLocation.CLLocationManager

/**
 * GMapViewModel view-model
 * Request for access user geolocation
 * Promise alert
 * Move to show info viewcontroller
 */

class GMapViewModel : ViewModel{
    
    // MARK: Properties
    private var userGeolocation : CLLocation?
    
    var currentLocation : CLLocation {
        
        get {
            guard userGeolocation != nil  else{
                
                return CLLocation()
            }
            
            return userGeolocation!
        }
        set {
            userGeolocation = newValue
        }
    }
    
    // MARK: Methods
    
  
    /**
     * Get Geolocation user data
     * Check authorization, procced different allowing access
     */
  
    func workWithUserLocation(){
        
        CLLocationManager.requestAuthorization().then { status -> LocationPromise in
            
            switch status{
                
            case .AuthorizedAlways:
                
                // Get Location when user allow access
                CLLocationManager.promise().then{ location  in
                    
                    // Save geolocation data
                    self.currentLocation = location
                    self.delegate?.viewModelDidUpdate()
                    
                    // Swift bug
                    return AnyPromise(bound: Promise<Void>(Void()))
                    
                    }.error{ error in
                        
                        let description : NSError = error as NSError
                        self.showResponseErrorMessage(description.localizedDescription)
                }
                
            case .Denied:
                
                // Processing denied access. Need alert and lead to settigns device screen
                self.showResponseErrorMessage("You denied access to your location")
                
            default:
                break
            }
            
            return CLLocationManager.promise()
            
            }.error { (error) in
                
                let description : NSError = error as NSError
                self.showResponseErrorMessage(description.localizedDescription)
        }
    }
    
    
    
    /**
     * Standart display alert with service message
     */
    
    func showResponseErrorMessage(message:String){
        
        let alert = PMKAlertController(title: "Description", message: message, preferredStyle: .Alert)
        let okAction = alert.addActionWithTitle("OK")
        
        UIApplication.topViewController()?.promiseViewController(alert).then { (action) -> UIAlertAction in
            
            switch action {
            case okAction:
                
                print("Left action")
            default:
                okAction
            }
            
            return action
        }
    }
    
    /**
     * Show selected pin info
     */
    
    func tapMethod(){
        
        let pinInfoViewController = PinInfoViewController(nibName: "PinInfoViewController", bundle: nil)
        UIApplication.topViewController()?.navigationController?.pushViewController(pinInfoViewController, animated: true)
    }
}