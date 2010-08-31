ActionController::Routing::Routes.draw do |map|
  map.connect 'users/generate_phone_key', :controller => 'users', :action => 'generate_phone_key'

  map.connect 'users/remove_phone_key', :controller => 'users', :action => 'remove_phone_key'

  map.connect 'card/:first_name.:last_name', :controller => 'users', :action => 'businesscard'
 
  map.connect 'vcard/:first_name.:last_name.vcf', :controller => 'users', :action => 'vcard'

  #App connections
  map.connect 'app/login.:format', :controller => 'users', :action => 'app_login'
  map.connect 'app/contactlist.:format', :controller => 'users', :action => 'app_contactlist'
  map.connect 'app/app_update_user_info.:format', :controller => 'users', :action => 'app_update_user_info'
  map.connect 'app/user_locations_and_status.:format', :controller => 'users', :action => 'app_user_locations_and_status'

  map.resources :users, :collection => { :edit => [:get,:post], :login => [:get,:post], :logout => [:get]  }

  # The priority is based upon order of creation: first created -> highest priority.

  # Sample of regular route:

  # Keep in mind you can assign values other than :controller and :action

  # Sample of named route:
  #   map.purchase 'products/:id/purchase', :controller => 'catalog', :action => 'purchase'
  # This route can be invoked with purchase_url(:id => product.id)

  # Sample resource route (maps HTTP verbs to controller actions automatically):
  #   map.resources :products

  # Sample resource route with options:
  #   map.resources :products, :member => { :short => :get, :toggle => :post }, :collection => { :sold => :get }

  # Sample resource route with sub-resources:
  #   map.resources :products, :has_many => [ :comments, :sales ], :has_one => :seller
  
  # Sample resource route with more complex sub-resources
  #   map.resources :products do |products|
  #     products.resources :comments
  #     products.resources :sales, :collection => { :recent => :get }
  #   end

  # Sample resource route within a namespace:
  #   map.namespace :admin do |admin|
  #     # Directs /admin/products/* to Admin::ProductsController (app/controllers/admin/products_controller.rb)
  #     admin.resources :products
  #   end

  # You can have the root of your site routed with map.root -- just remember to delete public/index.html.
  map.root :controller => "users"

  # See how all your routes lay out with "rake routes"

  # Install the default routes as the lowest priority.
  # Note: These default routes make all actions in every controller accessible via GET requests. You should
  # consider removing or commenting them out if you're using named routes and resources.
  map.connect ':controller/:action/:id'
  map.connect ':controller/:action/:id.:format'
end
