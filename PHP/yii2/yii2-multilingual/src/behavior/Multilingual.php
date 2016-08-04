<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\behavior;

use gbksoft\multilingual\models\Language;
use yii\base\Behavior;
use yii\base\InvalidConfigException;
use yii\db\ActiveQuery;
use yii\db\ActiveRecord;
use yii\helpers\ArrayHelper;

/**
 * Class Multilingual
 */
class Multilingual extends Behavior
{
    /**
     * @var ActiveRecord|Multilingual
     */
    public $owner;

    /**
     * @var string
     */
    public $translationModelName;

    /**
     * @var string
     */
    public $translationOwnerField;

    /**
     * @var string
     */
    public $languageField;

    /**
     * @var string
     */
    private $ownerPrimaryKey;

    /**
     * @inheritdoc
     */
    public function events()
    {
        return [
//            ActiveRecord::EVENT_AFTER_FIND => 'afterFind',
//            ActiveRecord::EVENT_AFTER_UPDATE => 'afterUpdate',
//            ActiveRecord::EVENT_AFTER_INSERT => 'afterInsert',
//            ActiveRecord::EVENT_AFTER_DELETE => 'afterDelete',
//            ActiveRecord::EVENT_BEFORE_VALIDATE => 'beforeValidate',
        ];
    }

    /**
     * @inheritdoc
     */
    public function attach($owner)
    {
        if (!$owner instanceof ActiveRecord) {
            throw new InvalidConfigException('Owner of this behavior must be extending ActiveRecord model.');
        }

        parent::attach($owner);

        $this->ownerPrimaryKey = $this->owner->primaryKey()[0];

        if (!isset($this->translationModelName, $this->translationOwnerField, $this->languageField)) {
            throw new InvalidConfigException(
                'Please specify contentModelName and contentOwnerKey in the '
                . get_class($this->owner)
                . ' configuration'
            );
        }

        $this->owner->getValidators();
    }

    /**
     * Relation to model translations
     *
     * @return ActiveQuery
     */
    public function getTranslations()
    {
        return $this->owner->hasMany(
            $this->translationModelName,
            [
                $this->translationOwnerField => $this->ownerPrimaryKey
            ]
        );
    }

    /**
     * Relation to model translation
     *
     * @param $locale
     * @return ActiveQuery
     */
    public function getTranslation($locale = null)
    {
        $locale = $locale ?: \Yii::$app->language;

        return $this->owner->hasOne(
            $this->translationModelName,
            [$this->translationOwnerField => $this->ownerPrimaryKey]
        )->from(['content' => call_user_func([$this->translationModelName, 'tableName'])])
            ->innerJoin(['lang' => Language::tableName()])
            ->where('lang.locale = :locale', [':locale' => $locale])
            ->andWhere('content.' . $this->owner->getDb()->quoteColumnName($this->languageField) . ' = lang.id');
    }

    /**
     * @param array $data
     * @param string|null $formName
     */
    public function saveTranslations($data = [], $formName = null)
    {
        $scope = $formName === null ? $this->owner->formName() : $formName;

        if ($scope === '' && !empty($data)) {
            $this->saveData($data);

            return;
        }

        if (isset($data[$scope])) {

            $this->saveData($data[$scope]);

            return;
        }
    }

    /**
     * @param array $data
     */
    private function saveData(array $data)
    {
        foreach ($this->getLanguages() as $locale => $language) {

            /** @var ActiveRecord $translation */
            $translation = $this->owner->getTranslation($locale)->one();
            if ($translation === null) {
                $translation = new $this->translationModelName();
                $translation->{$this->translationOwnerField} = $this->owner->{$this->ownerPrimaryKey};
                $translation->{$this->languageField} = $language->id;
            }

            if (isset($data[$locale])) {
                $translation->setAttributes($data[$locale]);
            }

            if (!$translation->save()) {
                $this->owner->addError($locale, $translation->getErrors());
            }
        }
    }

    /**
     * @return Language[]
     */
    private function getLanguages()
    {
        return ArrayHelper::index(Language::find()->all(), 'locale');
    }
}
