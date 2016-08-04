<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\models;

use gbksoft\multilingual\components\LanguageQueryFactory;
use gbksoft\multilingual\models\queries\LanguageQuery;
use yii\db\ActiveRecord;

/**
 * Class Language
 *
 * @property integer $id
 * @property string $url
 * @property string $locale
 * @property string $name
 * @property integer $is_default
 */
class Language extends ActiveRecord
{
    /**
     * @inheritdoc
     */
    public static function tableName()
    {
        return '{{%language}}';
    }

    /**
     * @inheritdoc
     */
    public function rules()
    {
        return [
            ['name', 'trim'],
            [['url', 'locale', 'name'], 'required'],
            [
                'locale',
                'match',
                'pattern' => '/^[a-z]{2}\-[A-Z]{2}$/',
                'message' => \Yii::t('language-backend', 'The Locale must match the pattern - <<[a-z]{2}\-[A-Z]{2}>>.'),
            ],
            [
                'url',
                'match',
                'pattern' => '/^[a-z]{2}$/',
                'message' => \Yii::t('language-backend', 'The Url must match the pattern - <<[a-z]{2}>>.'),
            ],
            ['url', 'unique'],
            ['locale', 'unique'],
            ['is_default', 'boolean'],
        ];
    }

    /**
     * @inheritdoc
     * @return LanguageQuery
     */
    public static function find()
    {
        return \Yii::createObject(LanguageQueryFactory::class)->create();
    }
}
