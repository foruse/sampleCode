import UIKit
import PromiseKit
import GoogleMaps

/**
 * GMapViewController show user location
 */

class GMapViewController: UIViewController {
    
    private var _viewModel : GMapViewModel!
    
    @IBOutlet private var _mapView : GMSMapView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view, typically from a nib.
        
        _viewModel = GMapViewModel()
        _viewModel.delegate = self
        
        _viewModel.workWithUserLocation()
        
        _mapView.delegate = self
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
        
    /**
     * Add GoogleMapView to GMapViewController
     */
    private func workWithGoogleMapsView(){
        
        // remove all markers from map
        _mapView.clear()
        
        // Set camera square location
        let camera = GMSCameraPosition(target: _viewModel.currentLocation.coordinate, zoom: 15, bearing: 0, viewingAngle: 0)
        _mapView.camera = camera
        
        // Creates a marker in the center of the map.
        let marker = GMSMarker(position: _viewModel.currentLocation.coordinate)
        marker.icon = UIImage(named: "map_pin")
        marker.snippet = "Your location"
        marker.appearAnimation = kGMSMarkerAnimationPop
        marker.map = _mapView
    }

}


/**
 * ViewModel delegate methods
 * update mapView camera position
 */

extension GMapViewController : ViewModelDelegate{
    
    func viewModelDidUpdate() {
        
        workWithGoogleMapsView()
    }
}

/**
 * GMSMapView delegate methods
 * tap by marker action
 */

extension GMapViewController : GMSMapViewDelegate {
    
    func mapView(mapView: GMSMapView, didTapInfoWindowOfMarker marker: GMSMarker) {
        
        _viewModel.tapMethod()
    }
}