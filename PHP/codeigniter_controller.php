<?php

/**
 * @author GBKSOFT team <hello@gbksoft.com>
 * @link http://gbksoft.com
 * @copyright 2011-2015 GBKSOFT team
 * @since 1.0
 *
 */

class Websites extends My_Controller {

    private $gallery;

    public function __construct() {
        parent::__construct();
        $this->load->model('notifications_model', 'notifications');
        $this->load->model('transactions');
    }

    public function copy($id) {
        $this->load->model('websites_model', 'websites');
        $this->load->model('tracker_model', 'tracker');

        $website = $this->websites->getWebsite($id);
        $new_website = $website;

        unset($new_website->id, $new_website->created);
        $orig_title = $new_website->title;
        $orig_name = $new_website->name;

        $new_website->title .= ' - copy';
        $new_website->name .= '_copy';

        if ($this->websites->checkSubdomainTaken($new_website->name)) {
            $counter = 1;
            while ($this->websites->checkSubdomainTaken($new_website->name)) {
                $new_website->name = $orig_name . '_copy_' . $counter;
                $new_website->title = $orig_title . ' - copy ' . $counter;

                $counter++;
            }
        }

        $new_website->created = date('Y-m-d H:i:s');
        $new_website->analytics_id = $this->tracker->createTracker($new_website->title, $new_website->name);

        $new_website->id = $this->websites->addWebsite($new_website);

        $this->notifications->addNotification($this->user_id, 'website', '"' . $orig_name . '" was copied and created as "' . $new_website->name . '".');

        $freeTransact['website_id'] = $new_website->id;
        $freeTransact['website_title'] = $new_website->title;
        $freeTransact['invoice'] = 0;
        $freeTransact['user_id'] = $this->tank_auth->get_user_id();
        $freeTransact['months'] = 0;
        $freeTransact['amount'] = 0;

        $this->transactions->addTransaction($freeTransact, date('Y-m-d H:i:s'));

        redirect('websites/listWebsites?copied=' . $new_website->title);
    }

    public function edit($id, $step = false) {
        $this->load->model('websites_model', 'websites');
        $this->load->model('templates');
        $this->load->model('components');
        $this->load->model('transactions');

        $website = $this->websites->getWebsite($id);

        if ($website->user_id != $this->tank_auth->get_user_id()) {
            redirect('/profile/index');
        }

        $data['website_id'] = $id;
        $data['just_edit'] = true;

        if (!$this->input->post('save_step') && $step != 2) {
            $data['t_info'] = $this->transactions->checkTransaction($id);

            $data['fields']['title'] = array(
                'label' => 'Your Website Address:',
                'name' => 'domain_name',
                'id' => 'title',
            );
            $data['fields']['name'] = array(
                'label' => 'Internal Website Address:',
                'name' => 'name',
                'id' => 'name',
            );
            $data['fields']['description'] = array(
                'label' => 'Comments:',
                'name' => 'description',
                'name' => 'description',
            );
            $data['fields']['meta_title'] = array(
                'label' => 'Title:',
                'name' => 'meta_title',
                'id' => 'meta_title',
            );
            $data['fields']['meta_keywords'] = array(
                'label' => 'Keywords:',
                'name' => 'meta_keywords',
                'id' => 'meta_keywords',
            );
            $data['fields']['meta_description'] = array(
                'label' => 'Description:',
                'name' => 'meta_description',
                'id' => 'meta_description',
            );

            $this->form_validation->set_rules('title', 'Your Website Address', 'required|min_length[3]|max_length[512]');
            $this->form_validation->set_rules('name', 'Internal Website Address', 'required|min_length[3]|max_length[255]|alpha_numeric');
            $this->form_validation->set_rules('meta_title', 'Title', 'required|min_length[3]|max_length[512]');

            $data['template'] = $this->templates->getTemplate($this->input->post('template_id'));
            $data['html'] = ($this->input->post('html')) ? $this->input->post('html') : $this->input->post('website');
            $data['website'] = $website;
            $data['back_step'] = '/websites/listWebsites';
            $this->addJS('jquery.alphanumeric.pack.js');
            $this->render('create_2', $data);
        } else if ($this->input->post('save_step') == 2 || $step == 2) {
            $this->addCSS('create_site.css');
            $this->addCSS('//fonts.googleapis.com/css?family=EB+Garamond|Quando|Cherry+Swash');
            $this->addJS('builder.js');
            $this->addJS('nicEdit.js');
            $this->addJS('base64.js');
            $this->addJS('jquery-ui-1.10.3.custom.min.js');
            $this->addJS('service.js');
            $this->addJS('live_site.js');
            $this->addJS('jquery.ui-jalert-ok.js');


            $template_id = $this->websites->getTemplateId($id);
            $template_id = $template_id->template;
            $this->addCSS(base_url() . "/user_files/templates_resources/$template_id/css/style.css");

            $gallery = $this->loadGallery($this->user_id);
            $user_photos = $this->getUserPhotos();
            $standart_img = $this->getStandartRemotePhotos();
            $data['upload_box'] = $this->gallery->showUploadBox(array('owner_photos' => $user_photos, 'website_id' => $id, 'standart_img' => $standart_img));

            $data['components'] = $this->components->getComponents();

            $data['code'] = $website->html;

            $template_id = $website->template;

            $data['template'] = $this->templates->getTemplate($template_id);
            $data['components_allowed'] = array_filter(explode(',', $data['template']->components_allowed));

            $data['similar_templates'] = $this->templates->findSimilarTemplates($template_id);
            $data['template'] = $this->templates->getTemplate($website->template);
            $data['components'] = $this->components->getComponents();
            $data['can_hide_header'] = true;
            $data['hide_top_bar'] = true;

            $this->load->model('friendship');
            $friends = $this->friendship->getFriends($this->user_id);

            if (is_array($friends) && $friends) {
                foreach ($friends as $key => $f) {
                    $friends[$key]->profile = $this->users_model->get_profile($f->to);

                    if (!empty($friends[$key]->profile->image)) {
                        $friends[$key]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
                    }
                    $friends[$key]->friends = $this->friendship->getFriends($f->to, $this->user_id);

                    foreach ($friends[$key]->friends as $skey => $sf) {
                        $friends[$key]->friends[$skey]->profile = $this->users_model->get_profile($sf->to);
                        if (empty($friends[$key]->friends[$skey]->profile->image)) {
                            if (!empty($friends[$key]->friends[$skey]->profile)) {
                                $friends[$key]->friends[$skey]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
                            }
                        }
                    }
                }
            }

            $data['friends'] = $friends;

            $data['max_file_size'] = $this->getMaxFileSize();

            $prev_data['title'] = $this->input->post('title');
            $prev_data['name'] = $this->input->post('name');
            $prev_data['domain_name'] = $this->input->post('domain_name');
            $prev_data['description'] = $this->input->post('description');
            $prev_data['meta_title'] = $this->input->post('meta_title');
            $prev_data['meta_keywords'] = $this->input->post('meta_keywords');
            $prev_data['meta_keywords'] = str_replace(' , ', ',', $prev_data['meta_keywords']);
            $prev_data['meta_keywords'] = str_replace(', ', ',', $prev_data['meta_keywords']);
            $prev_data['meta_keywords'] = str_replace(' ,', ',', $prev_data['meta_keywords']);
            $prev_data['meta_description'] = $this->input->post('meta_description');
            $data['prev_data'] = $prev_data;

            $data['user_email'] = $this->user_data->email;
            $data['user_id'] = $this->user_data->id;

            if ($this->input->post('save_final')) {
                $redirectTo = '';

                if ($this->input->post('create') == 'true') {
                    $this->load->model('transactions');
                    $last_payment = $this->transactions->getLastPaymentDate($this->user_data->id);
                    if (!$last_payment) {
                        $redirectTo = '/websites/listWebsites';
                    } else {
                        $redirectTo = '/billing/pay?wid=' . $id;
                    }
                } else {
                    $redirectTo = '/websites/listWebsites';
                }

                $this->load->model('websites_model', 'websites');
                $save_data['html'] = $this->input->post('website');
                $this->websites->updateWebsite($id, $save_data);
                $this->notifications->addNotification($this->user_id, 'website', 'Website "' . $website->title . '" edited.');
                $this->notifications->addNotification(0, 'website', 'Website "' . $website->title . '" edited by ' . $this->profile->name . ' ' . $this->profile->last_name . '.');
                redirect($redirectTo);
            } elseif ($this->input->post('method') == "edit") {
                $this->websites->updateWebsite($id, $prev_data);
                $this->notifications->addNotification($this->user_id, 'website', 'Website "' . $prev_data['title'] . '" edited.');
                $this->notifications->addNotification(0, 'website', 'Website "' . $prev_data['title'] . '" edited by ' . $this->profile->name . ' ' . $this->profile->last_name . '.');
                redirect('/websites/listWebsites');
            }

            $this->render('create_3', $data);
        }
    }

    public function remove($id, $info = false) {
        $this->load->model('websites_model', 'websites');
        $this->load->model("transactions");
        $website = $this->websites->getWebsite($id);

        if ($website->user_id != $this->tank_auth->get_user_id()) {
            redirect('websites/listWebsites');
        }

        $last_transact = $this->transactions->getTransactionLastIdWithNew($website->id);
        if (empty($last_transact)) {
            exit(false);
        }
        $transact_data = $this->transactions->getTransactionInfo($website->user_id, $last_transact[0]->id);
        $data_pay = ceil((time() - strtotime($transact_data->created)) / 2419200);
        if ($data_pay < (int) $website->recurring_month - $data_pay || $website->recurring_month == '1') {
            $data_month_amount = round(((int) $transact_data->amount / 12) * ((int) $website->recurring_month - $data_pay), 2);
            $this->sendRefundEmails($id, $this->user_id, $data_month_amount, $website->recurring_month);
            if ($info) {
                $this->transactions->updateTransactionDate($data_pay, $last_transact[0]->id, (int) $website->recurring_month);
                exit(true);
            }
        }

        $this->notifications->addNotification($this->user_id, 'website', 'Website "' . $website->title . '" deleted.');

        $this->websites->removeWebsite($id);

        redirect('websites/listWebsites');
    }

    public function get($name) {
        header('Content-Type: text/html; charset=utf-8');
        $this->dropCSS();
        $this->dropJS();
        $this->addCSS('live_site.css');
        $this->addCSS('gallery.css');
        $this->addCSS('preview_fix.css');
        $this->addCSS('template.css');
        $this->addCSS('ui-lightness/jquery-ui-1.10.3.custom.css');
        $this->addCSS('jquery.fancybox-1.3.4.css');
        $this->addCSS('jscrollpane.css');
        $this->addCSS('//fonts.googleapis.com/css?family=' . urlencode('EB Garamond|Quando|Cherry Swash'));
        $this->addJS('jquery.min.js');
        $this->addJS('gallery_component.js');
        $this->addJS('jquery.easing.min.js');
        $this->addJS('jquery.boxshadow.js');
        $this->addJS('jquery-ui-1.10.3.custom.min.js');
        $this->addJS('jquery.ui.touch-punch.min.js');
        $this->addJS('//maps.googleapis.com/maps/api/js?key=KEY&amp;sensor=true');
        $this->addJS('builder.js');
        $this->addJS('live_site.js');
        $this->addJS('html2canvas.js');
        $this->addJS('service.js');
        $this->addJS('jquery.fancybox.pack.js');
        $this->addJS('jquery.jcarousel.js');
        $this->addJS('jquery_file_uploader/jquery.iframe-transport.js');
        $this->addJS('jquery_file_uploader/jquery.fileupload.js');
        $this->addJS('jquery.ui-jalert.js');
        $this->addJS('base64.js');
        $this->addJS('jquery.mousewheel.js');
        $this->addJS('jquery.scrollpane.min.js');

        $this->load->model('websites_model', 'websites');
        $website = $this->websites->getWebsiteByName($name);
        $template_id = $this->websites->getTemplateId($website->id);
        $template_id = $template_id->template;
        $this->addCSS(base_url() . "/user_files/templates_resources/$template_id/css/style.css");

        if (!$website) {
            redirect('index/404');
        }

        if (!$website->published) {
            $this->render(false, false, 'template_nopublish');
            exit;
        }

        preg_match_all("/%each '([0-9]*)'%([\s\S]*)%\-each%/", $website->html, $matches);

        $website_data = (array) json_decode($website->data);

        if ($matches[0]) {
            foreach ($matches[0] as $key => $value) {
                $comp_id = $matches[1][$key];

                $each_replacement = '';

                if (!$website_data || !$website_data['comp_' . $comp_id]) {
                    $website_data['comp_' . $comp_id] = array();
                }

                foreach ($website_data['comp_' . $comp_id] as $p_key => $p_value) {
                    $p_value_keys = array();
                    foreach ($p_value as $p_value_key => $p_value_value) {
                        $p_value_keys[] = '$' . $p_value_key . '$';
                    }

                    $p_value_values = array_values((array) $p_value);

                    $each_replacement .= str_replace($p_value_keys, $p_value_values, $matches[2][$key]);
                }

                $website->html = str_replace($matches[2][$key], $each_replacement, $website->html);
            }

            $website->html = str_replace(array("%each '" . $comp_id . "'%", "%-each%"), '', $website->html);
        }

        $website->html = str_replace("&quot;", "'", $website->html);

        $data['profile'] = $this->users_model->get_profile($website->user_id);

        if ($this->user_data) {
            if ($this->user_id == $website->user_id) {
                $data['own'] = true;
            } else {
                $data['own'] = false;
            }
        } else {
            $data['own'] = false;
        }

        if ($data['own'] == false) {
            $data['creator_profile'] = $this->users_model->get_profile($website->user_id);
        }

        $this->load->model('friendship');
        $friends = $this->friendship->getFriends($website->user_id);

        foreach ($friends as $key => $f) {
            $friends[$key]->profile = $this->users_model->get_profile($f->to);
            if (empty($friends[$key]->profile->image)) {
                $friends[$key]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
            }
            $friends[$key]->friends = $this->friendship->getFriends($f->to, $this->user_id);

            foreach ($friends[$key]->friends as $skey => $sf) {
                $friends[$key]->friends[$skey]->profile = $this->users_model->get_profile($sf->to);
                if (empty($friends[$key]->friends[$skey]->profile->image)) {
                    if (!empty($friends[$key]->friends[$skey]->profile)) {
                        $friends[$key]->friends[$skey]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
                    }
                }
            }
        }

        $data['friends'] = $friends;

        $this->loadGallery($website->user_id);
        $user_photos = $this->getUserPhotos();
        $standart_img = $this->getStandartRemotePhotos();
        $data['upload_box'] = $this->gallery->showUploadBox(array('owner_photos' => $user_photos, 'standart_img' => $standart_img));

        $this->load->model('components');
        $data['components'] = $this->components->getComponents();

        $this->load->model('templates');
        $template = $this->templates->getTemplate($website->template);
        $data['reset_styles'] = $template->reset_styles;

        $data['website_id'] = $website->id;
        $data['title'] = ($website->meta_title) ? $website->meta_title : $website->title;
        $data['meta_keywords'] = $website->meta_keywords;
        $data['meta_description'] = $website->meta_description;
        $data['content'] = htmlspecialchars_decode($website->html);
        $data['analytics_id'] = $website->analytics_id;

        $this->render(false, $data, 'template_site');
    }

    public function getPreview($id, $template = false, $template_id = false) {
        header('Content-Type: text/html; charset=utf-8');
        $code = $this->input->post('data');
        $this->dropCSS();
        $this->dropJS();

        $this->addJS('//maps.googleapis.com/maps/api/js?key=KEY&amp;sensor=true');
        $this->addCSS('//fonts.googleapis.com/css?family=' . urlencode('EB Garamond|Quando|Cherry Swash'));
        $this->addCSS('live_site.css');
        $this->addCSS('gallery.css');
        $this->addCSS('preview_fix.css');
        $this->addCSS('template.css');
        $this->addCSS('ui-lightness/jquery-ui-1.10.3.custom.css');
        $this->addCSS('jquery.fancybox-1.3.4.css');
        $this->addCSS('jscrollpane.css');
        $this->addJS('jquery.min.js');
        $this->addJS('gallery_component.js');
        $this->addJS('jquery.easing.min.js');
        $this->addJS('jquery.boxshadow.js');
        $this->addJS('jquery-ui-1.10.3.custom.min.js');
        $this->addJS('builder.js');
        $this->addJS('live_site.js');
        $this->addJS('html2canvas.js');
        $this->addJS('service.js');
        $this->addJS('jquery.fancybox.pack.js');
        $this->addJS('jquery_file_uploader/jquery.iframe-transport.js');
        $this->addJS('jquery_file_uploader/jquery.fileupload.js');
        $this->addJS('jquery.jcarousel.js');
        $this->addJS('iscroll.js');
        $this->addJS('jquery.ui-jalert.js');
        $this->addJS('base64.js');
        $this->addJS('jquery.mousewheel.js');
        $this->addJS('jquery.scrollpane.min.js');
        $this->addJS('preview_fix.js');

        $this->load->model('previews_model', 'previews');

        if ($template) {
            $this->load->model('templates');
            $code = $this->templates->getTemplate($id)->code;
            $this->addCSS(base_url() . "/user_files/templates_resources/$id/css/style.css");
        } else {
            $code = $this->previews->getPreview($id);
            $this->addCSS(base_url() . "/user_files/templates_resources/$template_id/css/style.css");
        }

        $data['content'] = $code;

        $this->load->model('friendship');
        $friends = $this->friendship->getFriends($this->user_id);

        foreach ($friends as $key => $f) {
            $friends[$key]->profile = $this->users_model->get_profile($f->to);
            if (empty($friends[$key]->profile->image)) {
                $friends[$key]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
            }
            $friends[$key]->friends = $this->friendship->getFriends($f->to, $this->user_id);

            foreach ($friends[$key]->friends as $skey => $sf) {
                $friends[$key]->friends[$skey]->profile = $this->users_model->get_profile($sf->to);
                if (!empty($friends[$key]->friends[$skey]->profile->image)) {

                } else {
                    if (!empty($friends[$key]->friends[$skey]->profile)) {
                        $friends[$key]->friends[$skey]->profile->image = '/assets/images/template/profile_picture_replacement_68.gif';
                    }
                }
            }
        }

        $data['friends'] = $friends;

        $this->load->model('components');
        $data['components'] = $this->components->getComponents();

        $this->loadGallery($this->user_id);
        $user_photos = $this->getUserPhotos();
        $standart_img = $this->getStandartRemotePhotos();
        $data['upload_box'] = $this->gallery->showUploadBox(array('owner_photos' => $user_photos, 'standart_img' => $standart_img));

        $this->render(false, $data, 'template_preview');
    }

    public function addData($website_id, $page_comp_id, $comp_id) {
        $page_comp_id = 'comp_' . $page_comp_id;
        $this->load->model('websites_model', 'websites');
        $website = $this->websites->getWebsite($website_id);

        $this->load->model('components');
        $component = $this->components->getComponentById($comp_id);

        $this->notifications->addNotification($website->user_id, 'info', 'Your website "' . $website->title . '" was updated via "' . $component->title . '" feature. <a href="' . site_url('/websites/get/' . $website->name) . '">Check it out now</a>.');

        $data_json = $website->data;

        $data = json_decode($data_json);

        $data->$page_comp_id = (array) $data->$page_comp_id;

        if (!$data->$page_comp_id) {
            $data->$page_comp_id = array();
        }

        array_push($data->$page_comp_id, $this->input->post());

        $this->websites->updateWebsite($website_id, array('data' => json_encode($data)));
    }

    public function getData($website_id, $comp_id) {
        $comp_id = 'comp_' . $comp_id;
        $this->load->model('websites_model', 'websites');
        $website = $this->websites->getWebsite($website_id);
        $data_json = $website->data;
        $data = json_decode($data_json);

        echo json_encode($data->$comp_id);
    }

    public function saveBlob($website_id) {
        $page_screenshot = $this->input->post('page_screenshot');

        $this->load->model('websites_model', 'websites');
        $this->websites->updateWebsite($website_id, array(
            'blobimage' => $page_screenshot
        ));
    }

    public function getSimilarTemplatesJSON($template_id) {
        if (!$template_id) {
            return false;
        }

        $this->load->model('templates');
        $similar_templates = $this->templates->findSimilarTemplates($template_id);

        if ($similar_templates) {
            echo json_encode($similar_templates);
        } else {
            echo json_encode(array('result' => 'error'));
        }
    }

    public function getSimilarTemplatesHTML($template_id) {
        if (!$template_id) {
            return false;
        }

        $this->load->model('templates');
        $data['template'] = $this->templates->getTemplate($template_id);
        $data['similar_templates'] = $this->templates->findSimilarTemplates($template_id);

        if ($data['similar_templates']) {
            $this->render('similar_templates', $data, 'template_ajax');
        } else {
            echo false;
        }
    }

    public function checksubdomain() {
        $this->load->model('websites_model', 'websites');

        if ($this->input->get('skip')) {
            $subdomain_taken = $this->websites->checkSubdomainTaken($this->input->post('name'), (int) $this->input->get('skip'));
        } else {
            $subdomain_taken = $this->websites->checkSubdomainTaken($this->input->post('name'));
        }

        echo json_encode(!$subdomain_taken);
    }

}
