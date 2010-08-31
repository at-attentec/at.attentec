# -*- coding: utf-8 -*-
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

require 'vpim/vcard'
require 'rqrcode'

class UsersController < ApplicationController
  before_filter :login_required_web, :except => ['app_login','app_contactlist','app_user_locations_and_status','app_update_user_info','login','businesscard','vcard','new','create']
  before_filter :login_required_app, :only => ['app_login','app_contactlist','app_user_locations_and_status','app_update_user_info']
  before_filter :app_touch_connected_at, :only => ['app_login','app_contactlist','app_user_locations_and_status','app_update_user_info']
  before_filter :create_new_filter, :only => ['new','create']
  before_filter :admin_filter, :only => ['create', 'update']
  before_filter :phone_key_filter, :only => ['generate_phone_key','remove_phone_key']
  before_filter :card_get_user, :only => ['vcard','businesscard']
  

  layout "users", :except => [:businesscard]

  # GET /users
  # GET /users.xml
  def index
    @users = User.all(:order => "UPPER(first_name) ASC")

    respond_to do |format|
      format.html # index.html.erb
      format.json  { render :json => @users.to_json(:except => [:hashed_password,:salt, :created_at, :updated_at, :administrator]) }
    end
  end

  def login
    if request.post?
      if session[:user] = User.authenticate(params[:user][:username],params[:user][:password])
        flash[:notice] = t :login_success #"Login successful"
        redirect_to_stored
      else
        flash[:warning] = t :login_failure #"Wrong username or password"
      end
    end
  end

  def logout
    session[:user] = nil
    flash[:notice] = t :logout_success #"Logout successful"
    # redirect_to_stored
    redirect_to :action => 'login'
  end

  # GET /users/1
  # GET /users/1.xml
  def show
    @user = User.find(params[:id])

    respond_to do |format|
      format.html # show.html.erb
      format.json  { render :json => @user.to_json(:except => [:hashed_password,:salt, :created_at, :updated_at, :administrator]) }
      format.js
    end
  end

  def create_new_filter 
    if !$ENABLE_REGISTER and (current_user.nil? or !current_user.administrator)
      flash[:warning] = t :admin_add_not_allowed #"You cannot create users."
      redirect_to :action => ""
      return false
    end
    return true
  end

  # GET /users/new
  # GET /users/new.xml
  def new
    @user = User.new

    respond_to do |format|
      format.html # new.html.erb
    end
  end

  # GET /users/1/edit
  def edit
    @user = User.find(params[:id])
  end

  # POST /users
  # POST /users.xml
  def create
    @user = User.new(params[:user])

    respond_to do |format|
      if @user.save
        if $ENABLE_REGISTER and (current_user.nil? or !current_user.administrator)
          #Also log in as the new user
          session[:user] = @user
        else
          #Do not log in as new user, as admin is creating the new user.
        end
        flash[:notice] = t :user_created #'User was successfully created.'
        format.html { redirect_to(@user) }
      else
        format.html { render :action => "new" }
      end
    end
  end


  def generate_phone_key
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = current_user
    end
    if @user_editable
      @user.generate_phone_key
      flash[:notice] = t :phone_key_generated #'Phone key was generated'
    end
    respond_to do |format|
      format.html { redirect_to(@user) }
    end
  end

  def remove_phone_key
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = current_user
    end
    if @user_editable
      @user.remove_phone_key
      flash[:notice] = t :phone_key_removed #'Phone key was removed'
    end
    respond_to do |format|
      format.html { redirect_to(@user) }
    end
  end

  def businesscard_svg_qr
    def svg_qr_generate(code, options={})
      def draw_square(x,y,wh)
        return '<rect x="' + x.to_s + '" y="' + y.to_s + '" height="' + wh.to_s + '" width="' + wh.to_s + '"/>'
      end
      def handle_color(color)
        if color =~ /^[0-9abcdef]{6}$/i
          color = '#' + color
        end
        return color
      end
      qr = RQRCode::QRCode.new( code, :size => 3, :level => :l )
      padding = (options[:padding] || 10).to_i
      x = padding
      y = padding
      wh = (options[:pixel_size] || 2).to_i

      color = options[:color] || "#900028"
      color = handle_color(color)

      background_color = options[:background_color]
      background_color = handle_color(background_color)

      crisp_edges = options[:crisp_edges]
      shape_rendering = ''
      if crisp_edges
        shape_rendering = 'shape-rendering:crispEdges;'
      end
      out = ''
      qr.to_s.each_line do |line|
        line.each_char do |char|
          # puts char+"\n"
        if char == "\n"
          x = padding
          y += wh
        elsif char == ' '
          x += wh
        elsif char == 'x'
          out << draw_square(x,y,wh)
          x += wh
        else
          puts "ERROR"
        end
      end
      end
      out_wh = (y + wh + padding).to_s
      header = '<?xml version="1.0" standalone="yes"?>
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:ev="http://www.w3.org/2001/xml-events" width="' + out_wh + '" height="' + out_wh + '">
      <defs><style type="text/css"><![CDATA[ rect {fill:' + color + ';' + shape_rendering + '} ]]></style></defs>' 
      if background_color
        header << '<rect style="fill:' + background_color + '" x="0" y="0" height="' + out_wh + '" width="' + out_wh + '"/>'
      end

      out = header + out
      out << '</svg>'
      return out
    end
    
    # send_data card.to_s, :filename => @user.first_name+' '+@user.last_name+'.vcf', :type => 'text/x-vcard'
    @user = User.find(params[:id])
    
    
    respond_to do |format|
      format.svg { render :inline =>  svg_qr_generate(@template.business_card_link(@user.first_name, @user.last_name),params) }
    end
  end

  def card_get_user
    if !$ENABLE_BUSINESSCARD
      render :inline => (t :function_disabled)
      return
    end
    @user = User.find(:first,:conditions => ["first_name LIKE ? AND last_name LIKE ?", params[:first_name], params[:last_name]])
    if !@user.nil? and @user.hide_business_card
      @user = nil
    end
  end
  
  def businesscard
    respond_to do |format|
      format.html
    end
  end
  

  def vcard
    if @user.nil?
      respond_to do |format|
        format.html { render :inline => (t 'users.businesscard.name_not_found') }
      end
      return
    end
    card = Vpim::Vcard::Maker.make2 do |maker|

      #Add name
      maker.add_name do |name|
        name.given = @user[:first_name]
        name.family = @user[:last_name]
      end
      
      #Add Phone number
      if !@user[:phone].blank?
        maker.add_tel(@user[:phone]) do |t|
          t.location = 'work'
          t.preferred = true
        end
      end

      #Add email
      if !@user[:email].blank?
        maker.add_email(@user[:email]) { |e| e.location = 'work' }
      end

      #title
      if !@user[:title].blank?
        maker.title = @user[:title]
      end
      
      ## This works for some Vcard Applications, e.g. Google Contacts, but not for Mac OS X Addressbook.
      ## The problem is that the application wants to have equaly long chunks of base64 coded lines, 
      ## But the first line is shorter and not but on a new line when vPim creates them.
      ## The solution is to generate the image by hand which is done below
      # photo_path = @user.photo.path(:vcard)
      # if photo_path
      #   maker.add_photo do |photo|
      #     photo.image = File.open(photo_path).read
      #     photo.type = 'jpeg'
      #   end
      # end

      #Add Linked In if there is one
      if !@user[:linkedin_url].blank?
        maker.add_url(@user[:linkedin_url])
      end
      

      #Add Organization and webpage (static)
      maker.org = (t :company_long)
      maker.add_url(t :company_website)
    end

    ## Generate PHOTO section by hand, since vPims function does not work correct in all programs
    ## Creds to http://www.meeho.net/blog/2009/12/om-vpim-og-billeder-i-vcards/ for the tip.
    photo_path = @user.photo.path(:vcard)
    if photo_path
      photo_data = [File.open(photo_path).read].pack('m').to_s.gsub(/[ \n]/, '').scan(/.{1,76}/).join("\n ")
      card = card.encode.sub("END:VCARD", "PHOTO;BASE64:\n " + photo_data + "\nEND:VCARD")
    end

    send_data card.to_s, :filename => @user.first_name+' '+@user.last_name+'.vcf', :type => 'text/x-vcard'
  end
  
  # PUT /users/1
  # PUT /users/1.xml
  def update
    @user = User.find(params[:id])

    respond_to do |format|
      if @user.update_attributes(params[:user])
        flash[:notice] = t :user_updated #'User was successfully updated.'
        format.html { redirect_to(@user) }
      else
        format.html { render :action => "edit" }
      end
    end
  end
  
  def phone_key_filter
    if params[:id]
      if current_user.id != params[:id].to_i and !current_user.administrator
        flash[:warning] = t :user_not_editable #"This is not your user"
        @user_editable = false
      else
        @user_editable = true
      end
    end
  end
  
  def admin_filter
    if params[:user]
      #post data
      if current_user
        #logged in
        if current_user.administrator
          #logged in is admin
          if current_user.id == params[:id].to_i and params[:user][:administrator] == '0' # Efterlyses snyggare sätt att göra det här
            params[:user][:administrator] = true
            flash[:warning] = t :admin_remove_denied #'You can not remove your admin status'
          end
        else
          #logged in is not admin
          if params[:id].to_i != current_user.id
            flash[:warning] = t :user_not_editable #"This is not your user"
            redirect_to :action => ""
          end
          if params[:user][:administrator] == '1'
            flash[:warning] = t :admin_access_denied #'You can not be admin'
          end
          params[:user][:administrator] = false
        end
      else
        #not logged in
        params[:user][:administrator] = false
      end
    end
  end

  # DELETE /users/1
  # DELETE /users/1.xml
  def destroy
    if !current_user.administrator
      flash[:warning] = t :admin_remove_not_allowed #"You cannot remove users."
      redirect_to :action => ""
      return
    elsif current_user.id == params[:id].to_i
      flash[:warning] = t :self_remove_not_allowed #"You cannot remove yourself."
      redirect_to :action => ""
      return
    end
    @user = User.find(params[:id])
    @user.destroy
    
    respond_to do |format|
      format.html { redirect_to(users_url) }
    end
  end

  # App

  # Whenever app connects make sure you tell that you where here.
  def app_touch_connected_at
    current_user.touch([:connected_at])
  end

  def app_login
    respond_to do |format|
      format.json  { render :json => {"Responsestatus" => "Success"} }
    end
  end

  def app_contactlist
    @users = User.all
    json = render :json => {:users => @users,"Responsestatus" => "Success"}
    # p json
    respond_to do |format|
      format.json  { json }
    end
  end

  def app_user_locations_and_status
    @users = User.locations_and_status(current_user[:id])
    respond_to do |format|
      format.json  { render :json => {:users => @users,"Responsestatus" => "Success"} }
    end
  end


  def app_update_user_info #update Location and/or Status
    sucess = false
    data = {}
    @user = current_user

    if params[:location]
      begin
        location = ActiveSupport::JSON.decode(params[:location]);
        # Pick up only those fields that should you should be able to update to prevent someone to make themselves admin
        if location['longitude'] and location['latitude']
          @user.app_update_location!
          data['longitude'] = location['longitude']
          data['latitude'] = location['latitude']
          got_data = true
        end
      rescue TypeError,StandardError => e
        # Do nothing succes will still be false
      end
    end

    if params[:status]
        begin
          status = ActiveSupport::JSON.decode(params[:status]);
          # Pick up only those fields that should you should be able to update to prevent someone to make themselves admin
          if status['status'] and status['status_custom_message']
            data['status'] = status['status']
            data['status_custom_message'] = status['status_custom_message']
            got_data = true
          end
        rescue TypeError,StandardError => e
          # Do nothing succes will still be false
        end
      end
  
    begin
      if got_data
        if @user.update_attributes(data)
          @user.touch(:location_updated_at)
          success = true
        end
      end
    rescue NoMethodError => e
      # Do nothing succes will still be false
    end
    
    respond_to do |format|
      if success
        format.json  { render :json => {"Responsestatus" => "Success"} }
      else
        format.json  { render :json => {"Responsestatus" => "Failure"} }
      end
    end
  end
  
end
