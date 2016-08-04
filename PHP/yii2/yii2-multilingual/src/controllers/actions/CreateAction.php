<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\controllers\actions;

use gbksoft\multilingual\models\Language;
use yii\base\Action;
use yii\web\Response;

/**
 * Class CreateAction
 */
class CreateAction extends Action
{
    /**
     * @return string|Response
     */
    public function run()
    {
        /** @var Language $model */
        $model = new Language();

        if ($model->load(\Yii::$app->request->post()) && $model->save()) {

            return $this->controller->redirect(['update', 'id' => $model->id]);
        }

        return $this->controller->render('create', ['model' => $model]);
    }
}
