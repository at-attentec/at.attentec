#
# Copyright (c) 2010 Attentec AB, http://www.attentec.se
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'test_helper'

class UsersControllerTest < ActionController::TestCase
  # test "should get index" do
  #   get :index
  #   assert_response :success
  #   assert_not_nil assigns(:users)
  # end

  test "should get new" do
    $ENABLE_REGISTER = true
    get :new
    assert_response :success #since you allowed to access if ENABLE_REGISTER is true

    $ENABLE_REGISTER = false
    get :new
    assert_response :redirect #since you are not allow to access new if you are not admin
  end

  test "should create user" do
    #login as admin user longbob
    post :login, :user=>{ :username => "longbob", :password => "longtest"}

    assert_difference('User.count') do
      post :create, :user => {
        :username=>'egon', 
        :password => 'egonegon',
        :password_confirmation => 'egonegon',
        :email => 'egon@egon.com',
        :first_name => 'egon',
        :last_name => 'egonson'
      }
    end
    assert_redirected_to user_path(assigns(:user))
  end

  test "should redirect to login" do
    get :show, :id => users(:bob).to_param
    assert_response :redirect
    assert_redirected_to :controller => 'users', :action => 'login'
  end
  
  test "should get redirected to login as well" do
    get :edit, :id => users(:bob).to_param
    assert_response :redirect
    assert_redirected_to :controller => 'users', :action => 'login'
  end
  
  test "should get redirected to login for the third time" do
    put :update, :id => users(:bob).to_param, :user => { }
    assert_response :redirect
    assert_redirected_to :controller => 'users', :action => 'login'
  end
  
  test "should not destroy user, not logged in" do
    assert_difference('User.count', 0) do #You should not destroy, you are not allowed destroy without being logged in
      delete :destroy, :id => users(:bob).to_param
    end
    assert_redirected_to :controller => 'users', :action => 'login'
  end

  test "user should not destroy user" do
    post :login, :user=> { :username => @bob.username, :password => "test" }
    assert @response.has_session_object?(:user), "User could not login"
    assert_difference('User.count', 0) do #You should not destroy, you are not allowed destroy if you are normal user.
      delete :destroy, :id => users(:existingbob).to_param
    end
  end

  test "admin should be able to destroy other user" do
    #login as admin
    post :login, :user=> { :username => @longbob.username, :password => "longtest" }
    assert_difference('User.count', -1) do
      delete :destroy, :id => users(:bob).to_param
    end
    assert_difference('User.count', -1) do
      delete :destroy, :id => users(:existingbob).to_param
    end
    assert_difference('User.count', -1) do
      delete :destroy, :id => users(:emptybob).to_param
    end
  end
  
  test "admin should not be able to destroy self" do
    #login as admin
    post :login, :user=> { :username => @longbob.username, :password => "longtest" }
    assert_difference('User.count', 0) do
      delete :destroy, :id => users(:longbob).to_param
    end
  end
  
  
  self.use_instantiated_fixtures  = true

  fixtures :users

  def setup
    @controller = UsersController.new
    @request    = ActionController::TestRequest.new
    @response   = ActionController::TestResponse.new
    @request.host = "localhost"
  end

  
  
  def test_auth_bob
    #check we can login
    post :login, :user=> { :username => @bob.username, :password => "test" }
    assert @response.has_session_object?(:user)
    assert_equal @bob, session[:user]
    assert_response :redirect
    assert_redirected_to :controller => 'users', :action => ''
  end

  def test_enable_register_true
    $ENABLE_REGISTER = true
      #Try to signup without being signed in
      get :new
      assert_response :success
      #Send create request
      post :create, :user => { :username => "newbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" }
      assert_response :redirect
      assert @response.has_session_object?(:user)
      #Logout
      get :logout
      assert_response :redirect
      assert !@response.has_session_object?(:user)
  end
  
  def test_enable_register_false
    $ENABLE_REGISTER = false
      #Try to signup without being signed in
      get :new
      assert_response :redirect
      #Send create request
      post :create, :user => { :username => "newbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" }
      assert_response :redirect
      assert !@response.has_session_object?(:user)

      #login as regular user bob and try to reach signup
      post :login, :user=>{ :username => "bob", :password => "test"}
      assert_response :redirect
      assert @response.has_session_object?(:user)
      #Get new page
      get :new 
      assert_response :redirect
      #Send create request
      post :create, :user => { :username => "newbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" }
      assert_response :redirect
      assert_match /users\/"/,@response.body 
      assert @response.has_session_object?(:user)
      assert_equal "bob", session[:user][:username] #Should be the same as before
      #Logout
      get :logout
      assert_response :redirect
      assert !@response.has_session_object?(:user)

      #login as admin user longbob
      post :login, :user=>{ :username => "longbob", :password => "longtest"}
      assert_response :redirect
      assert @response.has_session_object?(:user)
      #Get new page
      get :new
      assert_response :success #Should succeed as admin
      #Send create request
      post :create, :user => { :username => "newbob", :password => "newpassword", :password_confirmation => "newpassword", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" }
      assert_response :redirect
      assert_match /users\/\d+"/,@response.body 
      assert @response.has_session_object?(:user)
      assert_equal "longbob", session[:user][:username] #Should be the same as before
      #Logout
      get :logout
      assert_response :redirect
      assert !@response.has_session_object?(:user)

  end
  
  def test_bad_signup
    #login as admin user longbob
    post :login, :user=>{ :username => "longbob", :password => "longtest"}

    #check we can't signup without all required fields
    post :create, :user => { 
     :username => "newbob",
     :password => "newpassword",
     :password_confirmation => "wrong" ,
     :email => "newbob@mcbob.com"
    }
    assert_response :success
    assert_template "users/new"
    assert_equal "longbob", session[:user][:username]

    post :create, :user => {
     :username => "yo",
     :password => "newpassword",
     :password_confirmation => "newpassword" ,
     :email => "newbob@mcbob.com"
    }
    assert_response :success
    assert_template "users/new"
    # assert_nil session[:user]

    post :create, :user => {
     :username => "yo",
     :password => "newpassword",
     :password_confirmation => "wrong" ,
     :email => "newbob@mcbob.com"
    }
    assert_response :success
    assert_template "users/new"
    # assert_nil session[:user]
  end
  
  def test_invalid_login
    #can't login with incorrect password
    post :login, :user=> { :username => "bob", :password => "not_correct" }
    assert_response :success
    assert !@response.has_session_object?(:user)
    assert flash[:warning]
    assert_template "users/login"
  end
  
  
  def test_login_logoff
    #login
    post :login, :user=>{ :username => "bob", :password => "test"}
    assert_response :redirect
    assert @response.has_session_object?(:user)
  
    #test updating user without password
    post :update, :user => { :username => "bob_is_getting_cooler", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" },  :id => 1000001
    assert_response :redirect
    assert @response.has_session_object?(:user)
    #then logoff
    get :logout
    assert_response :redirect
    assert !@response.has_session_object?(:user)
  
  
    #and log in again
    post :login, :user=>{ :username => "bob_is_getting_cooler", :password => "test"}
    assert_response :redirect
    assert @response.has_session_object?(:user)
    assert_equal "bob_is_getting_cooler",session[:user].username
  end
  
  def test_login_admin_change_user
    #login with longbob who is admin
    post :login, :user=>{
      :username => "longbob",
      :password => "longtest"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
  
    assert session[:user].administrator
    #test updating user without password
    post :update, :user => { :username => "admin_was_here", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" },  :id => 1000001
    assert_response :redirect
    #check that the username was changed
    get :show, :id => 1000001
    assert_match 'admin_was_here',@response.body 
    
  
    #then logoff
    get :logout
    assert_response :redirect
    assert !@response.has_session_object?(:user)
  end
  
  def test_login_not_admin_change_user
    #login with bob who is not admin
    post :login, :user=>{
      :username => "bob",
      :password => "test"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
  
    assert !session[:user].administrator
    #test updating user without password
    post :update, :user => { :username => "not_admin_was_here", :email => "newbob@mcbob.com", :first_name => "bobby", :last_name => "bobson" },  :id => 1000002
    assert_response :redirect
    #check that the username was not changed
    get :show, :id => 1000002
    assert_no_match /not_admin_was_here/,@response.body 
  
    #then logoff
    get :logout
    assert_response :redirect
    assert !@response.has_session_object?(:user)
  end
  
  
  def test_login_not_admin_become_admin
    #login with bob who is not admin
    post :login, :user=>{
      :username => "bob",
      :password => "test"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
  
    assert !session[:user].administrator
    #test updating user without password
    post :update, :user => {
      :username => "bob",
      :email => "newbob@mcbob.com",
      :first_name => "bobby",
      :last_name => "bobson",
      :administrator => "1"
    },  :id => 1000001
    assert_response :redirect
    assert !session[:user].administrator
  
    #then logoff
    get :logout
    assert_response :redirect
    assert !@response.has_session_object?(:user)
    
    #login again
    post :login, :user=>{
      :username => "bob",
      :password => "test"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
    assert !session[:user].administrator
  end
  
  def test_login_admin_remove_admin
    #login with longbob who is not admin
    post :login, :user=>{
      :username => "longbob",
      :password => "longtest"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
  
    assert session[:user].administrator
    #test updating user without password
    post :update, :user => {
      :username => "longbob",
      :email => "newbob@mcbob.com",
      :first_name => "bobby",
      :last_name => "bobson",
      :administrator => "0"
    },  :id => 1000003
    assert_response :redirect
    assert session[:user].administrator
  
    #then logoff
    get :logout
    assert_response :redirect
    assert !@response.has_session_object?(:user)
    
    #login again
    post :login, :user=>{
      :username => "longbob",
      :password => "longtest"
    }
    assert_response :redirect
    assert @response.has_session_object?(:user)
    assert session[:user].administrator
  end
  
  # 
  # def test_forgot_password
  #   #we can login
  #   post :username, :user=>{ :username => "bob", :password => "test"}
  #   assert_response :redirect
  #   assert @response.has_session_object?(:user)
  #   #logout
  #   get :logout
  #   assert_response :redirect
  #   assert !@response.has_session_object?(:user)
  #   #enter an email that doesn't exist
  #   post :forgot_password, :user => {:email=>"notauser@doesntexist.com"}
  #   assert_response :success
  #   assert !@response.has_session_object?(:user)
  #   assert_template "user/forgot_password"
  #   assert flash[:warning]
  #   #enter bobs email
  #   post :forgot_password, :user => {:email=>"exbob@mcbob.com"}   
  #   assert_response :redirect
  #   assert flash[:message]
  #   assert_redirected_to :action=>'login'
  # end
  # 
  def test_login_required
    #can't access index if not logged in
    get :index
    assert flash[:warning]
    assert_response :redirect
    assert_redirected_to :controller => 'users',  :action => 'login'
    #login
    post :login, :user=>{ :username => "bob", :password => "test"}
    assert_response :redirect
    assert @response.has_session_object?(:user)
    #can access it now
    get :index
    assert_response :success
    # assert flash.empty?
    assert_template "users/index.html.erb"
  end
  
  
  
  #App tests
  def test_app_login
    #check we can login
    post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    assert_equal @response.body, "{\"Responsestatus\":\"Success\"}"
    
    post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => "hejsan" })
    assert_equal @response.body, "{\"Responsestatus\":\"Wrong login\"}"
    
    post :app_login, :phone_auth => ''
    assert_equal @response.body, "{\"Responsestatus\":\"Wrong login\"}"
  
    get :app_login, :phone_auth => ''
    assert_equal @response.body, "{\"Responsestatus\":\"Wrong login\"}"
  end
  
  
  def test_app_contactlist
    #not logging in
    post :app_contactlist, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => 'hejsan' })
    assert_equal @response.body, "{\"Responsestatus\":\"Wrong login\"}"
  
    #no coordinates
    post :app_contactlist, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    decoded_repsonse = ActiveSupport::JSON.decode(@response.body)
    assert_equal 'Success', decoded_repsonse['Responsestatus']
    assert_equal 4, decoded_repsonse['users'].count
    # var_dump decoded_repsonse['users']
  end

  #App tests
  def test_app_update_location
    #not logging in
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => 'hejsan' })
    assert_equal "{\"Responsestatus\":\"Wrong login\"}", @response.body

    #no coordinates
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body


    #only latitude
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :location => ActiveSupport::JSON.encode({:latitude => '2.7182'})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    #only longitude
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :location => ActiveSupport::JSON.encode({:longitude => '3.14159'})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    #dum coordinates
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :longitude => 'x', :latitude => 'y'
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    #dum coordinates
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :longitude => 'x', :latitude => 'y'
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    #correct coordinates
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :location => ActiveSupport::JSON.encode({:latitude => '2.7182', :longitude => '3.14159'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    #fail coordinates
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :location => 'Failure'
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body


    #login
    post :login, :user=>{ :username => @bob.username, :password => "test"}
    assert_response :redirect
    assert @response.has_session_object?(:user)
    assert_equal(2.7182, @response.session[:user][:latitude])
    assert_equal(3.14159, @response.session[:user][:longitude])
  end
  
  def test_app_update_status
    #no status
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    #
    # Correct statuses
    #
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => ''})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => 'xyz'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'offline', :status_custom_message => 'xyz'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'invisible', :status_custom_message => 'This is longer.'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body
    
    #
    # Wrong statuses
    #
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'xyz', :status_custom_message => ''})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => 'x' * 26}) #Test max 25 chars
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => 'x' * 25}) #Test max 25 chars
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body


    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'Too long text in this box', :status_custom_message => ''})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online'})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status_custom_message => 'online'})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:administrator => '1'})
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => 'x'
    assert_equal "{\"Responsestatus\":\"Failure\"}", @response.body

    # 
    # 
    # #login
    # post :login, :user=>{ :username => @bob.username, :password => "test"}
    # assert_response :redirect
    # assert @response.has_session_object?(:user)
    # assert_equal(2.7182, @response.session[:user][:latitude])
    # assert_equal(3.14159, @response.session[:user][:longitude])
  end
  
  
  def test_app_update_status_and_location
    #
    # Correct status and location
    #
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => ''}), :location => ActiveSupport::JSON.encode({:latitude => '2.7182', :longitude => '3.14159'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    #
    # Only one of them is OK, Should still get success
    #
    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => ActiveSupport::JSON.encode({:status => 'online', :status_custom_message => ''}), :location => 'x'
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body

    post :app_update_user_info, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), :status => 'x', :location => ActiveSupport::JSON.encode({:latitude => '2.7182', :longitude => '3.14159'})
    assert_equal "{\"Responsestatus\":\"Success\"}", @response.body




    # 
    # 
    # #login
    # post :login, :user=>{ :username => @bob.username, :password => "test"}
    # assert_response :redirect
    # assert @response.has_session_object?(:user)
    # assert_equal(2.7182, @response.session[:user][:latitude])
    # assert_equal(3.14159, @response.session[:user][:longitude])
  end
  
  def test_app_user_locations
    #Check that we start without locations in the database
    post :app_user_locations_and_status, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    decoded = ActiveSupport::JSON.decode(@response.body)
    assert_not_nil(decoded, "Could not decode json")
    assert_equal(0, decoded['users'].length)
    
    2.times do |n| #Update the location twice for bob, should not see this since we cant see our own location
      post  :app_update_user_info, 
      :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key }), 
      :location => ActiveSupport::JSON.encode({:latitude => '1.7182', :longitude => '1.14159'})
      assert_equal "{\"Responsestatus\":\"Success\"}", @response.body
      
      post :app_user_locations_and_status, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
      decoded = ActiveSupport::JSON.decode(@response.body)
      assert_not_nil(decoded, "Could not decode json")
      assert_equal(0, decoded['users'].length)
    end
    
    2.times do |n| #Update the location twice for longbob, should now see longbobs location with bob
      post  :app_update_user_info, 
      :phone_auth => ActiveSupport::JSON.encode({ :username => @longbob.username, :phone_key => @longbob.phone_key }), 
      :location => ActiveSupport::JSON.encode({:latitude => '2.7182', :longitude => '3.14159'})
      assert_equal "{\"Responsestatus\":\"Success\"}", @response.body
      
      post :app_user_locations_and_status, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
      decoded = ActiveSupport::JSON.decode(@response.body)
      assert_not_nil(decoded, "Could not decode json")
      assert_equal(1, decoded['users'].length)
    end
    assert_not_nil(decoded['users'][0]['user']['location_updated_at'])
    assert_equal(3.14159, decoded['users'][0]['user']['longitude'])
    assert_equal(2.7182, decoded['users'][0]['user']['latitude'])
    
  end
  
  def test_app_wrong_phone_key_multiple
    assert_equal nil, @bob.failed_app_logins
    5.times do
      post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => "hejsan" })
      assert_equal "{\"Responsestatus\":\"Wrong login\"}", @response.body
    end
    post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
    assert_equal "{\"Responsestatus\":\"Wrong login\"}", @response.body
  end

  def test_app_wrong_phone_key_multiple_times_but_correct_in_between
    assert_equal nil, @bob.failed_app_logins
    3.times do
      4.times do
        post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => "hejsan" })
        assert_equal "{\"Responsestatus\":\"Wrong login\"}", @response.body
      end
      post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @bob.username, :phone_key => @bob.phone_key })
      assert_equal "{\"Responsestatus\":\"Success\"}", @response.body
    end
  end


  def test_app_empty_phone_key
    post :app_login, :phone_auth => ActiveSupport::JSON.encode({ :username => @emptybob.username, :phone_key => "" })
    assert_equal "{\"Responsestatus\":\"Wrong login\"}", @response.body
  end
  
  
  # Businesscard / vcard
  
  def quote_and_regexp(str)
    Regexp.new(Regexp.escape(str))
  end
  
  def test_correct_business_card_enabled
    $ENABLE_BUSINESSCARD = true
    #bob is not hidden
    post :businesscard, :first_name => @bob.first_name, :last_name => @bob.last_name
    assert_not_equal I18n.t(:function_disabled), @response.body
    assert_no_match quote_and_regexp(I18n.t('users.businesscard.name_not_found')), @response.body
    assert_match @bob.first_name+" "+@bob.last_name, @response.body

    #longbob is hidden
    post :businesscard, :first_name => @longbob.first_name, :last_name => @longbob.last_name
    assert_not_equal I18n.t(:function_disabled), @response.body
    assert_match I18n.t('users.businesscard.name_not_found'), @response.body
  end
  def test_correct_business_card_disabled
    $ENABLE_BUSINESSCARD = false
    #bob is not hidden
    post :businesscard, :first_name => @bob.first_name, :last_name => @bob.last_name
    assert_equal I18n.t(:function_disabled), @response.body

    #longbob is hidden
    post :businesscard, :first_name => @longbob.first_name, :last_name => @longbob.last_name
    assert_equal I18n.t(:function_disabled), @response.body
  end
  
  def test_correct_vcard_enabled
    $ENABLE_BUSINESSCARD = true
    #bob is not hidden
    post :vcard, :first_name => @bob.first_name, :last_name => @bob.last_name
    
    assert_equal 'text/x-vcard', @response.header['Content-Type']
    assert_not_equal I18n.t(:function_disabled), @response.body
    assert_no_match quote_and_regexp(I18n.t('users.businesscard.name_not_found')), @response.body
    assert_match @bob.first_name+" "+@bob.last_name, @response.body
    
    #longbob is hidden
    post :vcard, :first_name => @longbob.first_name, :last_name => @longbob.last_name
    assert_not_equal 'text/x-vcard', @response.header['Content-Type']
    assert_equal 'text/html', @response.header['Content-Type']
    assert_equal I18n.t('users.businesscard.name_not_found'), @response.body
  end
  
  def test_correct_vcard_disabled
    $ENABLE_BUSINESSCARD = false
    #bob is not hidden
    post :vcard, :first_name => @bob.first_name, :last_name => @bob.last_name
    assert_equal I18n.t(:function_disabled), @response.body
    
    #longbob is hidden
    post :vcard, :first_name => @longbob.first_name, :last_name => @longbob.last_name
    assert_equal I18n.t(:function_disabled), @response.body
    assert_equal 'text/html; charset=utf-8', @response.header['Content-Type']
  end
  
  def test_incorrect_business_card_enabled
    $ENABLE_BUSINESSCARD = true
    post :businesscard, :first_name => 'x', :last_name => 'y'
    assert_match I18n.t('users.businesscard.name_not_found'), @response.body
  end

  def test_incorrect_business_card_disabled
    $ENABLE_BUSINESSCARD = false
    post :businesscard, :first_name => 'x', :last_name => 'y'
    assert_equal I18n.t(:function_disabled), @response.body
  end

  def test_incorrect_vcard_enabled
    $ENABLE_BUSINESSCARD = true
    post :vcard, :first_name => 'x', :last_name => 'y'
    assert_match I18n.t('users.businesscard.name_not_found'), @response.body
    assert_equal 'text/html; charset=utf-8', @response.header['Content-Type']
  end

  def test_incorrect_vcard_disabled
    $ENABLE_BUSINESSCARD = false
    post :vcard, :first_name => 'x', :last_name => 'y'
    assert_equal I18n.t(:function_disabled), @response.body
    assert_equal 'text/html; charset=utf-8', @response.header['Content-Type']
  end
  
  def test_business_card_called_by_id
    #You should not be able to search by id (i.e. not be able to list users)
    $ENABLE_BUSINESSCARD = true
    post :businesscard, :id => 1000001
    assert_match I18n.t('users.businesscard.name_not_found'), @response.body
    assert_equal 'text/html; charset=utf-8', @response.header['Content-Type']
  end
  
  def test_vcard_called_by_id
    #You should not be able to search by id (i.e. not be able to list users)
    $ENABLE_BUSINESSCARD = true
    post :vcard, :id => 1000001
    assert_match I18n.t('users.businesscard.name_not_found'), @response.body
    assert_equal 'text/html; charset=utf-8', @response.header['Content-Type']
  end


end
