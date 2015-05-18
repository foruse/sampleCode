<?php

/**
 * @author GBKSOFT team <hello@gbksoft.com>
 * @link http://gbksoft.com
 * @copyright 2011-2015 GBKSOFT team
 * @since 1.0
 *
 */

/**
 * This is the model class for table "comments".
 *
 * The followings are the available columns in table 'comments':
 * @property integer $id
 * @property integer $tagId
 * @property integer $postId
 * @property integer $userId
 * @property integer $replyCommentId
 * @property string $text
 * @property string $image
 * @property string $time
 * @property integer $read
 *
 * The followings are the available model relations:
 * @property MetaTags $tag
 * @property Posts $post
 * @property Users $user
 * @property Comments $replyCcomment
 */

class Comments extends CActiveRecord
{
    /*
     * @var string verify code (captcha)
     */
    public $verifyCode;
    public $config = array(
        'path' => 'webroot.uploads',
        'url' => '/uploads/',
        'allowedExtensions' => 'jpg,jpeg,png,gif',
        'mimeTypes' => 'image/gif, image/jpeg, image/png',
        'minSize' => 0,
        'maxSize' => 2097152, /* 2 MB */
    );

    /*
     * @var string search text
     */
    public $userSearch;

    /**
     * @return string the associated database table name
     */
    public function tableName()
    {
        return 'comments';
    }

    /**
     * @return array validation rules for model attributes.
     */
    public function rules()
    {
        return array(
            array(
                'postId, userId, text, time',
                'required'
            ),
            array(
                'tagId, replyCommentId, postId, userId, read',
                'numerical',
                'integerOnly' => true
            ),
            array(
                'replyCommentId',
                'exist',
                'className' => 'Comments',
                'attributeName' => 'id',
                'message' => 'Reciver not found.'
            ),
            array(
                'image',
                'length',
                'max' => 255
            ),
            array(
                'image',
                'file',
                'allowEmpty' => true,
                'types' => $this->config['allowedExtensions'],
                'mimeTypes' => $this->config['mimeTypes'],
                'minSize' => $this->config['minSize'],
                'maxSize' => $this->config['maxSize']
            ),
            array(
                'imageTh',
                'file',
                'allowEmpty' => true,
                'types' => $this->config['allowedExtensions'],
                'mimeTypes' => $this->config['mimeTypes'],
                'minSize' => $this->config['minSize'],
                'maxSize' => $this->config['maxSize']
            ),
            array(
                'verifyCode',
                'captcha',
                'on' => 'insert, save, update, updatesave',
                'captchaAction' => '/ajax/commentCaptcha',
                'allowEmpty' => !CCaptcha::checkRequirements()
            ),
            array(
                'text',
                'length',
                'max' => postHelper::getSymbolLimit()
            ),
            array('text',
                'application.components.MHtmlPurifierFilter'
            ),
            array(
                'text',
                'filter',
                'on' => 'save, updatesave',
                'filter' => array($this, 'censor')
            ),
            array(
                'text',
                'filter',
                'on' => 'save, updatesave',
                'filter' => array($this, 'links')
            ),
            // The following rule is used by search().
            array(
                'tagId, postId, userId, text, userSearch',
                'safe',
                'on' => 'search'
            ),
        );
    }

    /**
     * @inheritdoc
     */
    protected function afterDelete()
    {
        $this->deleteImages();
        parent::afterDelete();
    }

    /**
     * Remove images
     */
    public function deleteImages()
    {
        if ($this->image) {
            $imagePath = Yii::getPathOfAlias($this->config['path']) . DIRECTORY_SEPARATOR . $this->image;
            if (file_exists($imagePath)) {
                unlink($imagePath);
            }
        }
        if ($this->imageTh) {
            $imagePath = Yii::getPathOfAlias($this->config['path']) . DIRECTORY_SEPARATOR . $this->imageTh;
            if (file_exists($imagePath)) {
                unlink($imagePath);
            }
        }
    }

    /**
     * @inheritdoc
     */
    public function relations()
    {
        return array(
            'tag' => array(self::BELONGS_TO, 'MetaTags', 'tagId'),
            'post' => array(self::BELONGS_TO, 'Posts', 'postId'),
            'user' => array(self::BELONGS_TO, 'Users', 'userId'),
            'replyComment' => array(self::BELONGS_TO, 'Comments', 'replyCommentId'),
        );
    }

    /**
     * @inheritdoc
     */
    public function defaultScope()
    {
        return array(
            'order' => 'time DESC',
        );
    }

    /**
     * Remove censor
     * @param type $value
     * @return String $value without censor words
     */
    public function censor($value)
    {
        return postHelper::replaceCensor($value);
    }

    /**
     *
     * @param type $value
     * @return String Html link
     */
    public function links($value)
    {
        return postHelper::urlToLink($value);
    }

    /**
     * @return array customized attribute labels (name=>label)
     */
    public function attributeLabels()
    {
        return array(
            'id' => 'ID',
            'tagId' => 'Tag',
            'postId' => 'Post',
            'userId' => 'User',
            'replyCommentId' => 'Reply Comment',
            'text' => 'Text',
            'image' => 'Image',
            'time' => 'Time',
            'read' => 'Read',
            'userSearch' => 'User',
        );
    }

    /**
     * Retrieves a list of models based on the current search/filter conditions.
     *
     * Typical usecase:
     * - Initialize the model fields with values from filter form.
     * - Execute this method to get CActiveDataProvider instance which will filter
     * models according to data in model fields.
     * - Pass data provider to CGridView, CListView or any similar widget.
     *
     * @return CActiveDataProvider the data provider that can return the models
     * based on the search/filter conditions.
     */
    public function search()
    {
        $criteria = new CDbCriteria;

        $criteria->compare('tagId', $this->tagId);
        $criteria->compare('postId', $this->postId);
        $criteria->compare('userId', $this->userId);
        $criteria->compare('text', $this->text, true);

        //search in admin panel
        $criteria->with = array('user');
        $criteria->compare('user.email', $this->userSearch, true);

        return new CActiveDataProvider($this, array(
            'criteria' => $criteria,
            'pagination' => array('pageSize' => 20),
            'sort' => array(
                'attributes' => array(
                    'user_search' => array(
                        'asc' => 'user.email',
                        'desc' => 'user.email DESC',
                    ),
                    '*',
                ),
            ),
        ));
    }

    /**
     * Returns the static model of the specified AR class.
     * Please note that you should have this exact method in all your CActiveRecord descendants!
     * @param string $className active record class name.
     * @return Comments the static model class
     */
    public static function model($className = __CLASS__)
    {
        return parent::model($className);
    }

    /**
     * Validate upload image
     * @param CUploadedFile $uploadFile
     * @return string|boolean
     */
    public function checkImageUpload(CUploadedFile $uploadFile)
    {
        if (($uploadFile->hasError == 1) || ($uploadFile->hasError == 2)) {
            $this->addError('image', 'The file "' . $uploadFile->getName() . '" is too large. Its size cannot exceed ' . postHelper::getMaximumFileUploadSize() . ' bytes.');
            return false;
        } elseif ($uploadFile->hasError) {
            //3-8
            $this->addError('image', 'Problems with upload. Contact administrator for help.');
            return false;
        }

        try {
            $config = $this->config;
            postHelper::createPath(Yii::getPathOfAlias($config['path']));
            $fileName = $uploadFile->getName() . "-" . mktime() . '.' . $uploadFile->getExtensionName();
            return $fileName;
        } catch (Exception $e) {
            $this->setErrors($e->getMessage());
        }

        return false;
    }

    /**
     * Save image to folder
     * @param type $newName New name of the file
     */
    public function saveImage($newName)
    {
        $saveTo = Yii::getPathOfAlias($this->config['path']) . DIRECTORY_SEPARATOR . $newName;
        $saveToThumb = Yii::getPathOfAlias($this->config['path']) . DIRECTORY_SEPARATOR . "th_" . $newName;

        $img = Yii::app()->imagine->getImagine()->open($this->image->tempName);
        $width = $img->getSize()->getWidth();
        if ($width >= 1100) {
            Yii::app()->imagine->resize($this->image->tempName, 1100, 1100)->save($saveTo);
        } else {
            $this->image->saveAs($saveTo);
        }
        //preview
        Yii::app()->imagine->thumb($saveTo, 110, 110)->save($saveToThumb);
        $this->image = $newName;
        $this->imageTh = "th_" . $newName;
    }

    /*
     * Remove comment
     */
    public function deleteComment()
    {
        $transaction = $this->dbConnection->beginTransaction();
        try {
            if ($this->delete()) {
                $transaction->commit();
            } else {
                $transaction->rollback();
            }
        } catch (Exception $e) {
            $transaction->rollback();
        }
    }
}
