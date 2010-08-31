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

# Filters added to this controller apply to all controllers in the application.
# Likewise, all the methods added will be available for all controllers.
#require 'activesupport/json'
class ApplicationController < ActionController::Base
  helper :all # include all helpers, all the time
  protect_from_forgery :only => [:update,:login,:create]# See ActionController::RequestForgeryProtection for details

  # Scrub sensitive parameters from your log
  filter_parameter_logging :password
  
  def login_required_web
    #Observe that return false in a filter stop the execution totolly, the method that this is filter for will still run.
    #redirect_to or render on the other hand, will interrupt the execution.
    if current_user #uses current_user so we check if the user still exists
      return true
    end
    session[:user] = nil
    flash[:warning] = t :please_login #"Please log in to continue"
    session[:return_to] = request.request_uri
    
    redirect_to :controller => "users", :action => "login"
    return false
  end

  def login_required_app
    #check if logged in via post data from phone
    if params[:phone_auth]
      authdata = ActiveSupport::JSON.decode(params[:phone_auth])
      begin
        @phone_authed_user = User.find(:first, :conditions => ["username = ?", authdata["username"]])
        if @phone_authed_user
          if !@phone_authed_user.phone_key.blank? and @phone_authed_user.phone_key == authdata["phone_key"]
            @phone_authed_user.login_failed_reset
            return true
          else
            @phone_authed_user.login_failed
          end
        end
      rescue
        #not successful in login from phone
      end
    end
    render :json => {"Responsestatus" => "Wrong login"}
    return false
  end

  def current_user
    if session[:user]
      begin
        return User.find(session[:user][:id])
      rescue #user was deleted
        return nil
      end
    elsif @phone_authed_user
      return @phone_authed_user
    end
    nil
  end
  
  def redirect_to_stored
    if return_to = session[:return_to]
      session[:return_to] = nil
      relative_url = ActionController::Base.relative_url_root || ""
      redirect_to relative_url + return_to
    else
      redirect_to :controller => 'users', :action => ''
    end
  end
  
  def is_authorized(user_id)
    if user_id == current_user.id
      return true
    end
    false
  end
end
