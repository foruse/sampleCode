<?php
/**
 * Team profile
 *
 * @author GBKSOFT team <hello@gbksoft.com>
 * @link http://gbksoft.com
 * @copyright 2011-2015 GBKSOFT team
 * @since 1.0
 *
 */

/**
 * TeamProfile - class access to team data. This will be one point to access
 * data of the team in the project
 */
class TeamProfile
{
    /*
     * Team id
     */
    protected $id;

    /*
     * Model AR on witch data is sets
     */
    protected $model;
    protected $isDataLoaded = FALSE;

    /*
     * Protected helpers
     */
    protected $location;
    protected $seasons;

    /*
     * Method to get attributes
     */
    public function __get($name)
    {
        $getter = 'get' . $name;
        if (method_exists($this, $getter))
        {
            return $this->$getter();
        } else
        {
            return $this->model->$name;
        }
        throw new CException('Property "{' . $name . '}" is not defined.');
    }

    /*
     * Get for internal use
     */
    protected function get($name)
    {
        if (property_exists($this, $name))
        {
            return $this->$name;
        } else
        {
            return $this->model->$name;
        }
        throw new CException('Property "{' . $name . '}" is not defined.');
    }

    public function __construct($teamId = NULL)
    {
        if (!is_null($teamId))
        {
            Yii::app()->getModule('team');
            $dependency = new CDbCacheDependency('SELECT updateTime FROM team WHERE id = ' . $teamId);
            $model = Team::model()->cache(24*60*60, $dependency)->with(array('stats'))->findByPk($teamId);
            if ($model)
            {
                $this->loadFromModel($model);
                $this->isDataLoaded = TRUE;
            }
        }
    }

    public function loadFromModel(CActiveRecord $model)
    {
        $this->model = $model;
        $this->id = (int) $model->id;
    }

    public function isDataLoaded()
    {
        return $this->isDataLoaded;
    }

    public function getSeasonBeginDate()
    {
        $v = (int) $this->get('seasonBeginMonth');
        if (!empty($v))
        {
            return date("d-m-Y", mktime(0, 0, 0, $v, 1, date("Y")));
        }

        return NULL;
    }

    public function getSeasonEndDate()
    {
        $v = (int) $this->get('seasonEndMonth');
        if (!empty($v))
        {
            return date("d-m-Y", mktime(0, 0, 0, ($v + 1), 0, date("Y")));
        }

        return NULL;
    }

    protected function getLocationData()
    {
        if (is_null($this->location))
        {
            $location = array();

            $city = $this->get('city');
            if (!empty($city))
            {
                $location['city'] = LocationHelper::cityName($city);
            }

            $state = $this->get('state');
            if (!empty($state))
            {
                $location['state'] = LocationHelper::stateName($state);
            }

            $country = $this->get('country');
            if (!empty($country))
            {
                $location['country'] = LocationHelper::countryName($country);
            }
        } else
        {
            $location = $this->location;
        }

        return $location;
    }

    public function getLocation()
    {
        $location = $this->getLocationData();
        return (empty($location) ? '' : implode(', ', $location));
    }

    public function getLocationMap()
    {
        $location = $this->getLocationData();
        if (isset($location['state']))
        {
            unset($location['state']);
        }
        return (empty($location) ? '' : implode(', ', $location));
    }

    public function getThumbUrl($defaultThumb = NULL)
    {
        if (!file_exists($defaultThumb))
        {
            $defaultThumb = NULL;
        }

        if (is_null($defaultThumb))
        {
            $defaultThumb = Yii::app()->theme->baseUrl . '/assets/img/lion.png';
        }

        $url = PhotoHelper::createTeamThumb('teamPhoto', $this->id, 'mainPhoto.png', 200, 200);
        if (empty($this->get('imageName')) || empty($url))
        {
            return Yii::app()->theme->baseUrl . '/assets/img/lion.png';
        }

        return $url;
    }

    public function getLastUploadedPhotoThumb()
    {
        $result = Yii::app()->getModule('upload')->getLastPhotoUrl($this->id, 'teamPhoto');

        if (FALSE === $result)
        {
            return Yii::app()->theme->baseUrl . '/assets/img/seeTeamPhoto.png';
        }

        return $result;
    }

    public function getLastUploadedVideoThumb()
    {
        $result = Yii::app()->getModule('upload')->getLastVideoUrl('team', $this->id);

        if (FALSE === $result)
        {
            return Yii::app()->theme->baseUrl . '/assets/img/seeTeamVideo.png';
        }

        return $result;
    }

    public function isTeamMember($teamId, $userId, $roleId)
    {
        return (bool) TeamUser::model()->countByAttributes(array('teamId' => $teamId, 'userId' => $userId, 'teamRoleId' => $roleId));
    }

    public function getUrl()
    {
        return Yii::app()->createUrl('/team/default/index', array('teamId' => $this->id));
    }

    public function getTeamLevelName()
    {
        if (is_null($this->teamlevel))
        {
            $this->teamlevel = TeamHelper::getPlayLevelFlag($this->id);
        }
        return $this->teamlevel;
    }

    public function getSeasons()
    {
        if (is_null($this->seasons))
        {
            $dependency = new CDbCacheDependency('SELECT count(*) FROM teamSeason WHERE teamId = ' . $this->id);
            $model = TeamSeason::model()->cache(24*60*60, $dependency)->findAllByAttributes(array('teamId' => $this->id));

            if (empty($model))
            {
                return array();
            }

            $this->seasons = array();
            foreach ($model as $season)
            {
                $this->seasons[] = $season->attributes;
            }
        }
        return $this->seasons;
    }

}
